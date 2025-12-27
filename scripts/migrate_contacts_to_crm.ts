/**
 * Phase 1 helper: migrate legacy tenant-scoped `Contact` records into CRM entities:
 * - `Company` (from business contacts + free-text `company` strings)
 * - `Person` (from individual contacts)
 *
 * Determinism / idempotency:
 * - Companies are deduped by `(orgId, normalizedName)` (active-only uniqueness).
 * - Persons are deduped by `externalId = legacy_contact:<contactId>` (new field) and will attempt
 *   safe linking to existing persons by `userId`, `primaryEmail`, or `ironworkerNumber` before creating.
 *
 * Usage (from backend repo):
 *   node --require ts-node/register/transpile-only scripts/migrate_contacts_to_crm.ts
 *
 * Env:
 *   MONGO_URI / MONGO_DB       # optional; defaults match app config
 *   ORG_ID="..."               # optional; migrate a single org
 *   DRY_RUN="1"                # optional; do not write, only report
 *   OUTPUT_PATH="report.json"  # optional; write JSON report to disk
 */
import * as fs from 'node:fs';
import * as path from 'node:path';
import { createConnection, Schema } from 'mongoose';

import { mongoConfig } from '../src/config/app.config';
import { TenantConnectionService } from '../src/common/tenancy/tenant-connection.service';
import { normalizeEmail, normalizeKeys, normalizeName, normalizePhoneE164 } from '../src/common/utils/normalize.util';

import { OrganizationSchema } from '../src/features/organizations/schemas/organization.schema';
import { ContactSchema } from '../src/features/contacts/schemas/contact.schema';
import { PersonSchema } from '../src/features/persons/schemas/person.schema';
import { CompanySchema } from '../src/features/companies/schemas/company.schema';

const LegacyInviteSchema = new Schema(
  {
    orgId: { type: String, index: true },
    email: { type: String },
    personId: { type: String, default: null },
    contactId: { type: String, default: null },
    archivedAt: { type: Date, default: null },
  },
  { timestamps: true, strict: false }
);

type Collision = {
  type: 'primaryEmail' | 'primaryPhoneE164' | 'ironworkerNumber' | 'reportsTo';
  key?: string;
  contactId: string;
  contactName: string;
  details?: Record<string, any>;
};

type OrgReport = {
  orgId: string;
  contactsScanned: number;
  companiesCreated: number;
  companiesReused: number;
  personsCreated: number;
  personsLinkedExisting: number;
  personsSkippedBusinessContacts: number;
  invitesBackfilled: number;
  reportsToLinked: number;
  collisions: Collision[];
};

type RunReport = {
  runAt: string;
  dryRun: boolean;
  mongo: { uri: string; dbName: string };
  orgs: OrgReport[];
};

const LEGACY_PERSON_EXTERNAL_PREFIX = 'legacy_contact:';

function nowIso() {
  return new Date().toISOString();
}

function outputPathDefault() {
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  return path.join(process.cwd(), `crm_migration_report_${stamp}.json`);
}

async function main() {
  const dryRun = process.env.DRY_RUN === '1' || process.env.DRY_RUN === 'true';
  const orgIdFilter = (process.env.ORG_ID || '').trim() || null;
  const outputPath = (process.env.OUTPUT_PATH || '').trim() || null;

  const sharedConn = await createConnection(mongoConfig.uri, { dbName: mongoConfig.dbName }).asPromise();
  sharedConn.model('Organization', OrganizationSchema);
  sharedConn.model('Invite', LegacyInviteSchema);
  sharedConn.model('Contact', ContactSchema);
  sharedConn.model('Person', PersonSchema);
  sharedConn.model('Company', CompanySchema);

  const orgModel = sharedConn.model('Organization');
  const inviteModel = sharedConn.model('Invite');
  const defaultContactModel = sharedConn.model('Contact');
  const defaultPersonModel = sharedConn.model('Person');
  const defaultCompanyModel = sharedConn.model('Company');

  const tenants = new TenantConnectionService(sharedConn as any, orgModel as any);

  const orgQuery: any = { archivedAt: null };
  if (orgIdFilter) orgQuery._id = orgIdFilter;
  const orgs: any[] = await orgModel.find(orgQuery).lean();

  const report: RunReport = {
    runAt: nowIso(),
    dryRun,
    mongo: { uri: mongoConfig.uri, dbName: mongoConfig.dbName },
    orgs: [],
  };

  for (const org of orgs) {
    const orgId = String(org._id || org.id);

    const orgConn = await tenants.getConnectionForOrg(orgId);
    const contactModel = await tenants.getModelForOrg(orgId, 'Contact', ContactSchema, defaultContactModel as any);
    const personModel = await tenants.getModelForOrg(orgId, 'Person', PersonSchema, defaultPersonModel as any);
    const companyModel = await tenants.getModelForOrg(orgId, 'Company', CompanySchema, defaultCompanyModel as any);

    const contacts: any[] = await contactModel
      .find({ orgId })
      .sort({ createdAt: 1, _id: 1 })
      .lean();

    const orgReport: OrgReport = {
      orgId,
      contactsScanned: contacts.length,
      companiesCreated: 0,
      companiesReused: 0,
      personsCreated: 0,
      personsLinkedExisting: 0,
      personsSkippedBusinessContacts: 0,
      invitesBackfilled: 0,
      reportsToLinked: 0,
      collisions: [],
    };

    if (!contacts.length) {
      report.orgs.push(orgReport);
      if (orgConn !== sharedConn) await tenants.resetConnectionForOrg(orgId);
      continue;
    }

    const companyByNormalized = new Map<string, string>();
    const resolveCompanyId = async (rawName: string | null | undefined) => {
      const name = String(rawName || '').trim();
      if (!name) return null;
      const normalizedName = normalizeName(name);
      if (!normalizedName) return null;

      if (companyByNormalized.has(normalizedName)) {
        return companyByNormalized.get(normalizedName) as string;
      }

      const existing = await companyModel.findOne({ orgId, normalizedName, archivedAt: null }).lean();
      if (existing?._id) {
        const id = String(existing._id);
        companyByNormalized.set(normalizedName, id);
        orgReport.companiesReused += 1;
        return id;
      }

      if (dryRun) {
        orgReport.companiesCreated += 1;
        const placeholder = `dryrun:${normalizedName}`;
        companyByNormalized.set(normalizedName, placeholder);
        return placeholder;
      }

      const created = await companyModel.create({
        orgId,
        name,
        normalizedName,
        externalId: null,
        website: null,
        mainEmail: null,
        mainPhone: null,
        companyTypeKeys: [],
        tagKeys: [],
        rating: null,
        notes: null,
        archivedAt: null,
        piiStripped: false,
        legalHold: false,
      });
      const id = String((created as any)._id || created.id);
      companyByNormalized.set(normalizedName, id);
      orgReport.companiesCreated += 1;
      return id;
    };

    // Pre-create companies deterministically (so person linking can reuse ids).
    for (const contact of contacts) {
      const kind = String(contact.contactKind || 'individual');
      if (kind === 'business') {
        await resolveCompanyId(contact.name);
      }
      if (contact.company) {
        await resolveCompanyId(contact.company);
      }
    }

    // Existing active uniqueness keys (avoid conflicts when creating active Persons).
    const existingActivePeople: any[] = await personModel
      .find({ orgId, archivedAt: null })
      .select({ _id: 1, primaryEmail: 1, primaryPhoneE164: 1, ironworkerNumber: 1, userId: 1, externalId: 1 })
      .lean();

    const activeEmailSet = new Set<string>(
      existingActivePeople.map((p) => String(p.primaryEmail || '').toLowerCase()).filter(Boolean)
    );
    const activePhoneSet = new Set<string>(
      existingActivePeople.map((p) => String(p.primaryPhoneE164 || '')).filter(Boolean)
    );
    const activeIronSet = new Set<string>(existingActivePeople.map((p) => String(p.ironworkerNumber || '')).filter(Boolean));

    const personByExternalId = new Map<string, any>();
    existingActivePeople
      .filter((p) => typeof p.externalId === 'string' && p.externalId.startsWith(LEGACY_PERSON_EXTERNAL_PREFIX))
      .forEach((p) => personByExternalId.set(String(p.externalId), p));

    const contactToPersonId = new Map<string, string>();

    const mapPersonType = (raw: any) => {
      const v = String(raw || 'external').trim().toLowerCase();
      if (v === 'staff') return 'internal_staff';
      if (v === 'ironworker') return 'internal_union';
      return 'external_person';
    };

    const safeTrim = (value: any) => {
      const v = typeof value === 'string' ? value.trim() : '';
      return v ? v : null;
    };

    const findExistingMatch = async (contact: any) => {
      const invitedUserId = safeTrim(contact.invitedUserId) || safeTrim(contact.foremanUserId);
      if (invitedUserId) {
        const existing = await personModel.findOne({ orgId, userId: invitedUserId }).lean();
        if (existing?._id) return existing;
      }
      const email = safeTrim(contact.email);
      if (email) {
        const normalized = normalizeEmail(email);
        const existing = await personModel.findOne({ orgId, primaryEmail: normalized, archivedAt: null }).lean();
        if (existing?._id) return existing;
      }
      const iron = safeTrim(contact.ironworkerNumber);
      if (iron) {
        const existing = await personModel.findOne({ orgId, ironworkerNumber: iron, archivedAt: null }).lean();
        if (existing?._id) return existing;
      }
      return null;
    };

    for (const contact of contacts) {
      const contactId = String(contact._id || contact.id);
      const contactName = String(contact.displayName || contact.name || '').trim() || `Contact ${contactId}`;
      const kind = String(contact.contactKind || 'individual');

      if (kind === 'business') {
        orgReport.personsSkippedBusinessContacts += 1;
        continue;
      }

      const externalId = `${LEGACY_PERSON_EXTERNAL_PREFIX}${contactId}`;

      const existingByExternal = personByExternalId.get(externalId);
      if (existingByExternal?._id) {
        contactToPersonId.set(contactId, String(existingByExternal._id));
        orgReport.personsLinkedExisting += 1;
        continue;
      }

      const matched = await findExistingMatch(contact);
      if (matched?._id) {
        const personId = String(matched._id);
        contactToPersonId.set(contactId, personId);
        orgReport.personsLinkedExisting += 1;

        if (!dryRun && !matched.externalId) {
          await personModel.updateOne({ _id: matched._id, orgId }, { $set: { externalId } });
          personByExternalId.set(externalId, { ...matched, externalId });
        }
        continue;
      }

      const archivedAt = contact.archivedAt ? new Date(contact.archivedAt) : null;
      const isActive = !archivedAt;

      const emailRaw = safeTrim(contact.email);
      const emailNormalized = emailRaw ? normalizeEmail(emailRaw) : null;

      const phoneRaw = safeTrim(contact.phone);
      const phoneE164 = phoneRaw ? normalizePhoneE164(phoneRaw) : null;

      const ironworkerNumber = safeTrim(contact.ironworkerNumber);

      const emailCollision = isActive && emailNormalized && activeEmailSet.has(emailNormalized.toLowerCase());
      const phoneCollision = isActive && phoneE164 && activePhoneSet.has(phoneE164);
      const ironCollision = isActive && ironworkerNumber && activeIronSet.has(ironworkerNumber);

      const emails = emailCollision || !emailNormalized ? [] : [{ value: emailRaw as string, normalized: emailNormalized, isPrimary: true }];
      const phones = phoneCollision || !phoneE164 ? [] : [{ value: phoneRaw as string, e164: phoneE164, isPrimary: true }];

      if (emailCollision && emailNormalized) {
        orgReport.collisions.push({ type: 'primaryEmail', key: emailNormalized, contactId, contactName });
      }
      if (phoneCollision && phoneE164) {
        orgReport.collisions.push({ type: 'primaryPhoneE164', key: phoneE164, contactId, contactName });
      }
      if (ironCollision && ironworkerNumber) {
        orgReport.collisions.push({ type: 'ironworkerNumber', key: ironworkerNumber, contactId, contactName });
      }

      const companyId = await resolveCompanyId(contact.company);

      const tagKeys = normalizeKeys(Array.isArray(contact.tags) ? contact.tags : []);
      const skillKeys = normalizeKeys(Array.isArray(contact.skills) ? contact.skills : []);

      const invitedUserId = safeTrim(contact.invitedUserId) || safeTrim(contact.foremanUserId);

      if (dryRun) {
        orgReport.personsCreated += 1;
        contactToPersonId.set(contactId, `dryrun:${contactId}`);
        if (emailNormalized && !emailCollision) activeEmailSet.add(emailNormalized.toLowerCase());
        if (phoneE164 && !phoneCollision) activePhoneSet.add(phoneE164);
        if (ironworkerNumber && !ironCollision) activeIronSet.add(ironworkerNumber);
        continue;
      }

      const created = await personModel.create({
        orgId,
        externalId,
        personType: mapPersonType(contact.personType),
        firstName: safeTrim(contact.firstName),
        lastName: safeTrim(contact.lastName),
        displayName: safeTrim(contact.displayName) || safeTrim(contact.name) || `Person ${contactId}`,
        dateOfBirth: contact.dateOfBirth ? new Date(contact.dateOfBirth) : null,
        emails,
        phones,
        tagKeys,
        skillKeys,
        departmentKey: null,
        orgLocationId: safeTrim(contact.officeId),
        reportsToPersonId: null,
        ironworkerNumber: ironCollision ? null : ironworkerNumber,
        unionLocal: safeTrim(contact.unionLocal),
        skillFreeText: Array.isArray(contact.skills) ? contact.skills.filter((s: any) => typeof s === 'string' && s.trim()) : [],
        certifications: Array.isArray(contact.certifications) ? contact.certifications : [],
        rating: typeof contact.rating === 'number' ? contact.rating : null,
        notes: safeTrim(contact.notes),
        companyId: companyId && !String(companyId).startsWith('dryrun:') ? companyId : null,
        companyLocationId: null,
        title: null,
        userId: invitedUserId,
        archivedAt,
        piiStripped: !!contact.piiStripped,
        legalHold: !!contact.legalHold,
      });

      const personId = String((created as any)._id || created.id);
      contactToPersonId.set(contactId, personId);
      orgReport.personsCreated += 1;

      if (emailNormalized && !emailCollision) activeEmailSet.add(emailNormalized.toLowerCase());
      if (phoneE164 && !phoneCollision) activePhoneSet.add(phoneE164);
      if (ironworkerNumber && !ironCollision) activeIronSet.add(ironworkerNumber);
    }

    // Second pass: reportsToContactId -> reportsToPersonId
    for (const contact of contacts) {
      if (!contact.reportsToContactId) continue;
      const contactId = String(contact._id || contact.id);
      const contactName = String(contact.displayName || contact.name || '').trim() || `Contact ${contactId}`;

      const personId = contactToPersonId.get(contactId);
      const managerId = contactToPersonId.get(String(contact.reportsToContactId));
      if (!personId || !managerId) {
        orgReport.collisions.push({
          type: 'reportsTo',
          contactId,
          contactName,
          details: { reportsToContactId: String(contact.reportsToContactId), resolvedPersonId: !!personId, resolvedManagerId: !!managerId },
        });
        continue;
      }
      if (personId === managerId) continue;

      orgReport.reportsToLinked += 1;
      if (dryRun) continue;

      const existing = await personModel.findOne({ _id: personId, orgId }).select({ reportsToPersonId: 1 }).lean();
      const current = existing?.reportsToPersonId ? String(existing.reportsToPersonId) : null;
      if (current && current !== managerId) {
        orgReport.collisions.push({
          type: 'reportsTo',
          contactId,
          contactName,
          details: { personId, currentReportsToPersonId: current, desiredReportsToPersonId: managerId },
        });
        continue;
      }
      await personModel.updateOne({ _id: personId, orgId }, { $set: { reportsToPersonId: managerId } });
    }

    // Backfill Invite.personId from Invite.contactId (system DB)
    const pendingInvites: any[] = await inviteModel
      .find({ orgId, contactId: { $ne: null }, personId: null, archivedAt: null })
      .lean();

    for (const inv of pendingInvites) {
      const contactId = String(inv.contactId || '');
      const personId = contactToPersonId.get(contactId);
      if (!personId || String(personId).startsWith('dryrun:')) continue;
      orgReport.invitesBackfilled += 1;
      if (dryRun) continue;
      await inviteModel.updateOne({ _id: inv._id }, { $set: { personId } });
    }

    report.orgs.push(orgReport);

    if (orgConn !== sharedConn) {
      await tenants.resetConnectionForOrg(orgId);
    }
  }

  if (outputPath || process.env.OUTPUT_PATH === '') {
    const finalPath = outputPath || outputPathDefault();
    fs.writeFileSync(finalPath, JSON.stringify(report, null, 2));
    // eslint-disable-next-line no-console
    console.log(`[crm migration] wrote report: ${finalPath}`);
  }

  // eslint-disable-next-line no-console
  console.log(`[crm migration] done (dryRun=${dryRun}). orgs=${report.orgs.length}`);
  report.orgs.forEach((o) => {
    console.log(
      `- orgId=${o.orgId} contacts=${o.contactsScanned} companies(created=${o.companiesCreated} reused=${o.companiesReused}) persons(created=${o.personsCreated} linked=${o.personsLinkedExisting}) invitesBackfilled=${o.invitesBackfilled} collisions=${o.collisions.length}`
    );
  });

  await sharedConn.close();
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(err);
  process.exit(1);
});
