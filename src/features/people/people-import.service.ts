import { BadRequestException, Injectable } from '@nestjs/common';
import { AuditLogService } from '../../common/services/audit-log.service';
import { Role } from '../../common/roles';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeEmail, normalizeName } from '../../common/utils/normalize.util';
import { Company, CompanySchema } from '../companies/schemas/company.schema';
import { CompanyLocation, CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { Office, OfficeSchema } from '../offices/schemas/office.schema';
import { Person, PersonSchema } from '../persons/schemas/person.schema';
import { PersonsService } from '../persons/persons.service';
import { UsersService } from '../users/users.service';
import { PeopleImportConfirmRowDto, PeopleImportRowDto } from './dto/people-import.dto';
import { PeopleImportV1ConfirmRowDto, PeopleImportV1RowDto } from './dto/people-import-v1.dto';
import { computeWithinFileDuplicateErrors } from './people-import-duplicates.util';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

type PreviewRowResult = {
  row: number;
  suggestedAction: 'create' | 'update' | 'error';
  personId?: string;
  contactId?: string;
  matchBy?: 'email' | 'ironworkerNumber';
  errors?: string[];
  warnings?: string[];
};

@Injectable()
export class PeopleImportService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    private readonly persons: PersonsService,
    private readonly users: UsersService
  ) {}

  private async personTenantModel(orgId: string) {
    return this.tenants.getModelForOrg<Person>(orgId, 'Person', PersonSchema);
  }

  private async companyTenantModel(orgId: string) {
    return this.tenants.getModelForOrg<Company>(orgId, 'Company', CompanySchema);
  }

  private async companyLocationTenantModel(orgId: string) {
    return this.tenants.getModelForOrg<CompanyLocation>(orgId, 'CompanyLocation', CompanyLocationSchema);
  }

  private async officeTenantModel(orgId: string) {
    return this.tenants.getModelForOrg<Office>(orgId, 'Office', OfficeSchema);
  }

  private normalizePersonType(value?: string) {
    const normalized = (value || '').trim().toLowerCase();
    if (normalized === 'internal_staff' || normalized === 'staff') return 'internal_staff';
    if (normalized === 'internal_union' || normalized === 'ironworker') return 'internal_union';
    if (normalized === 'external_person' || normalized === 'external') return 'external_person';
    return undefined;
  }

  private rowErrors(row: PeopleImportRowDto) {
    const errors: string[] = [];
    if (!row.name || !row.name.trim()) errors.push('name is required');
    const normalizedPersonType = this.normalizePersonType(row.personType);
    if (row.personType && !normalizedPersonType) {
      errors.push(`personType must be one of internal_staff|internal_union|external_person`);
    }
    return errors;
  }

  async preview(actor: ActorContext, rows: PeopleImportRowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import preview');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const model = await this.personTenantModel(orgId);
    const emails = Array.from(
      new Set(
        rows
          .map((row) => normalizeEmail(row.email || ''))
          .filter(Boolean)
      )
    );
    const ironworkerNumbers = Array.from(
      new Set(
        rows
          .map((row) => (row.ironworkerNumber || '').trim())
          .filter(Boolean)
      )
    );

    const orFilters: Record<string, any>[] = [
      ...(emails.length ? [{ primaryEmail: { $in: emails } }] : []),
      ...(ironworkerNumbers.length ? [{ ironworkerNumber: { $in: ironworkerNumbers } }] : []),
    ];

    const existing = orFilters.length
      ? await model
          .find({
            orgId,
            archivedAt: null,
            $or: orFilters,
          })
          .lean()
      : [];

    const emailMap = new Map<string, any>();
    const ironMap = new Map<string, any>();
    existing.forEach((person) => {
      if (person.primaryEmail) emailMap.set(String(person.primaryEmail).toLowerCase(), person);
      if (person.ironworkerNumber) ironMap.set(String(person.ironworkerNumber), person);
    });

    const previewRows: PreviewRowResult[] = rows.map((row) => {
      const errors = this.rowErrors(row);
      const warnings: string[] = [];

      const emailKey = normalizeEmail(row.email || '');
      const ironKey = (row.ironworkerNumber || '').trim();

      const matchedByEmail = emailKey ? emailMap.get(emailKey) : undefined;
      const matchedByIron = ironKey ? ironMap.get(ironKey) : undefined;

      if (matchedByEmail && matchedByIron && String(matchedByEmail._id) !== String(matchedByIron._id)) {
        errors.push('email and ironworkerNumber match different existing people');
      }

      const matched = matchedByEmail || matchedByIron;
      const matchBy = matchedByEmail ? 'email' : matchedByIron ? 'ironworkerNumber' : undefined;
      const matchedId = matched?._id ? String(matched._id) : undefined;

      if (matched?.archivedAt) {
        warnings.push('matches an archived person; update may fail unless unarchived first');
      }

      if (errors.length) {
        return { row: row.row, suggestedAction: 'error', personId: matchedId, contactId: matchedId, matchBy, errors, warnings };
      }

      if (matched) {
        return { row: row.row, suggestedAction: 'update', personId: matchedId, contactId: matchedId, matchBy, warnings };
      }

      return { row: row.row, suggestedAction: 'create' };
    });

    const summary = previewRows.reduce(
      (acc, r) => {
        acc.total += 1;
        if (r.suggestedAction === 'create') acc.creates += 1;
        else if (r.suggestedAction === 'update') acc.updates += 1;
        else acc.errors += 1;
        return acc;
      },
      { total: 0, creates: 0, updates: 0, errors: 0 }
    );

    await this.audit.logMutation({
      action: 'preview',
      entity: 'people_import',
      orgId,
      userId: actor.userId,
      metadata: {
        total: summary.total,
        creates: summary.creates,
        updates: summary.updates,
        errors: summary.errors,
      },
    });

    return { orgId, summary, rows: previewRows };
  }

  async confirm(actor: ActorContext, rows: PeopleImportConfirmRowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;
    if (!actor.userId) throw new BadRequestException('Missing actor context');
    if (!actor.role) throw new BadRequestException('Missing actor role');

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import confirm');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const results: Array<{ row: number; status: 'ok' | 'skipped' | 'error'; message?: string }> = [];
    let created = 0;
    let updated = 0;
    let skipped = 0;
    let invitesCreated = 0;
    let errors = 0;

    const companyModel = await this.companyTenantModel(orgId);
    const companyCache = new Map<string, string>();

    for (const row of rows) {
      if (row.action === 'skip') {
        skipped += 1;
        results.push({ row: row.row, status: 'skipped' });
        continue;
      }

      const validationErrors = this.rowErrors(row);
      if (validationErrors.length) {
        errors += 1;
        results.push({ row: row.row, status: 'error', message: validationErrors.join('; ') });
        continue;
      }

      const personType = this.normalizePersonType(row.personType) || 'external_person';
      const email = row.email ? normalizeEmail(row.email) : undefined;
      const phone = row.phone?.trim() || undefined;

      let companyId: string | null = null;
      const companyName = (row.company || '').trim();
      if (companyName) {
        const normalizedCompanyName = normalizeName(companyName);
        if (companyCache.has(normalizedCompanyName)) {
          companyId = companyCache.get(normalizedCompanyName) as string;
        } else {
          const existingCompany = await companyModel
            .findOne({ orgId, normalizedName: normalizedCompanyName, archivedAt: null })
            .lean();
          if (existingCompany?._id) {
            companyId = String(existingCompany._id);
          } else {
            const createdCompany = await companyModel.create({
              orgId,
              name: companyName,
              normalizedName: normalizedCompanyName,
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
            companyId = String((createdCompany as any)._id || createdCompany.id);
          }
          if (companyId) companyCache.set(normalizedCompanyName, companyId);
        }
      }

      const personPayload: any = {
        personType,
        displayName: row.name,
        dateOfBirth: row.dateOfBirth || undefined,
        emails: email ? [email] : undefined,
        primaryEmail: email,
        phones: phone ? [phone] : undefined,
        primaryPhone: phone,
        ironworkerNumber: row.ironworkerNumber || undefined,
        unionLocal: row.unionLocal || undefined,
        skillFreeText: row.skills || undefined,
        certifications: row.certifications || undefined,
        notes: row.notes || undefined,
        companyId: companyId || undefined,
      };

      try {
        if (row.action === 'create') {
          const createdPerson = await this.persons.create(actor, orgId, personPayload);
          created += 1;
          const personId = String((createdPerson as any)._id || (createdPerson as any).id);
          // Invites are intentionally not created during import; invites are managed explicitly via the Invites UI.

          results.push({ row: row.row, status: 'ok' });
          continue;
        }

        if (row.action === 'update') {
          const personId = row.personId || row.contactId;
          if (!personId) {
            throw new BadRequestException('personId is required for update rows');
          }
          await this.persons.update(actor, orgId, personId, personPayload);
          updated += 1;
          // Invites are intentionally not created during import; invites are managed explicitly via the Invites UI.

          results.push({ row: row.row, status: 'ok' });
          continue;
        }

        skipped += 1;
        results.push({ row: row.row, status: 'skipped' });
      } catch (err: any) {
        errors += 1;
        results.push({ row: row.row, status: 'error', message: err?.message || 'import_failed' });
      }
    }

    await this.audit.logMutation({
      action: 'confirm',
      entity: 'people_import',
      orgId,
      userId: actor.userId,
      metadata: {
        processed: rows.length,
        created,
        updated,
        skipped,
        invitesCreated,
        errors,
      },
    });

    return {
      orgId,
      processed: rows.length,
      created,
      updated,
      skipped,
      invitesCreated,
      errors,
      results,
    };
  }

  private escapeRegex(value: string) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  private derivePrimaryEmail(emails?: string[], primaryEmail?: string) {
    const clean = (emails || []).map((e) => normalizeEmail(e)).filter(Boolean);
    const deduped = Array.from(new Set(clean));
    if (!deduped.length) return { emails: [] as string[], primaryEmail: undefined as string | undefined };
    const desired = primaryEmail ? normalizeEmail(primaryEmail) : '';
    const chosen = desired && deduped.includes(desired) ? desired : deduped[0];
    return { emails: deduped, primaryEmail: chosen || undefined };
  }

  private splitFreeTextList(values?: string[]) {
    return (values || [])
      .map((value) => String(value || '').trim())
      .filter(Boolean);
  }

  async previewV1(actor: ActorContext, rows: PeopleImportV1RowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import preview');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const withinFileDuplicateErrors = computeWithinFileDuplicateErrors(rows);

    const primaryEmails = Array.from(
      new Set(
        rows
          .map((row) => this.derivePrimaryEmail(row.emails, row.primaryEmail).primaryEmail || '')
          .map((email) => normalizeEmail(email))
          .filter(Boolean)
      )
    );
    const ironworkerNumbers = Array.from(
      new Set(
        rows
          .map((row) => (row.ironworkerNumber || '').trim())
          .filter(Boolean)
      )
    );

    const model = await this.personTenantModel(orgId);
    const orFilters: Record<string, any>[] = [
      ...(primaryEmails.length ? [{ primaryEmail: { $in: primaryEmails } }] : []),
      ...(ironworkerNumbers.length ? [{ ironworkerNumber: { $in: ironworkerNumbers } }] : []),
    ];

    const existing = orFilters.length
      ? await model
          .find({
            orgId,
            archivedAt: null,
            $or: orFilters,
          })
          .lean()
      : [];

    const emailMap = new Map<string, any>();
    const ironMap = new Map<string, any>();
    existing.forEach((person) => {
      if (person.primaryEmail) emailMap.set(String(person.primaryEmail).toLowerCase(), person);
      if (person.ironworkerNumber) ironMap.set(String(person.ironworkerNumber), person);
    });

    const previewRows: PreviewRowResult[] = rows.map((row) => {
      const errors: string[] = [];
      const warnings: string[] = [];

      if (!row.displayName || !row.displayName.trim()) errors.push('displayName is required');
      if (!row.personType) errors.push('personType is required');

      const { emails, primaryEmail } = this.derivePrimaryEmail(row.emails, row.primaryEmail);
      if (row.primaryEmail && primaryEmail !== normalizeEmail(row.primaryEmail)) {
        errors.push('primaryEmail must be one of the emails values');
      }

      const ironKey = (row.ironworkerNumber || '').trim();
      const matchedByEmail = primaryEmail ? emailMap.get(String(primaryEmail).toLowerCase()) : undefined;
      const matchedByIron = ironKey ? ironMap.get(ironKey) : undefined;

      if (matchedByEmail && matchedByIron && String(matchedByEmail._id) !== String(matchedByIron._id)) {
        errors.push('primaryEmail and ironworkerNumber match different existing people');
      }

      const duplicateErrors = withinFileDuplicateErrors.get(row.row) || [];
      duplicateErrors.forEach((msg) => {
        if (!errors.includes(msg)) errors.push(msg);
      });

      const matched = matchedByEmail || matchedByIron;
      const suggestedAction = errors.length ? 'error' : matched ? 'update' : 'create';
      const matchBy = matchedByEmail ? 'email' : matchedByIron ? 'ironworkerNumber' : undefined;

      const companyName = (row.companyName || '').trim();
      const companyExternalId = (row.companyExternalId || '').trim();
      const companyLocationName = (row.companyLocationName || '').trim();
      const companyLocationExternalId = (row.companyLocationExternalId || '').trim();

      if ((companyLocationName || companyLocationExternalId) && !(companyName || companyExternalId)) {
        errors.push('company_name or company_external_id is required when company location is provided');
      }
      if (companyExternalId && !companyName) {
        warnings.push('company_external_id provided without company_name; importer can only link if the company already exists');
      }
      if (companyLocationExternalId && !companyLocationName) {
        warnings.push(
          'company_location_external_id provided without company_location_name; importer can only link if the location already exists'
        );
      }

      // Invites are intentionally not created during import; invites are managed explicitly via the Invites UI.

      return {
        row: row.row,
        suggestedAction: suggestedAction as any,
        personId: matched?._id ? String(matched._id) : undefined,
        contactId: matched?._id ? String(matched._id) : undefined,
        matchBy,
        errors: errors.length ? errors : undefined,
        warnings: warnings.length ? warnings : undefined,
      };
    });

    const summary = {
      total: rows.length,
      creates: previewRows.filter((r) => r.suggestedAction === 'create').length,
      updates: previewRows.filter((r) => r.suggestedAction === 'update').length,
      errors: previewRows.filter((r) => r.suggestedAction === 'error').length,
    };

    return { orgId, summary, rows: previewRows };
  }

  async confirmV1(actor: ActorContext, rows: PeopleImportV1ConfirmRowDto[]) {
    if (!actor.orgId) throw new BadRequestException('Missing organization context');
    const orgId = actor.orgId;

    if (!Array.isArray(rows) || rows.length === 0) {
      throw new BadRequestException('No rows supplied for import confirm');
    }
    if (rows.length > 1000) {
      throw new BadRequestException('Row limit exceeded (max 1000)');
    }

    const companyModel = await this.companyTenantModel(orgId);
    const companyLocationModel = await this.companyLocationTenantModel(orgId);
    const officeModel = await this.officeTenantModel(orgId);

    const companyByExternalId = new Map<string, string>();
    const companyByNormalizedName = new Map<string, string>();

    const locationByExternalId = new Map<string, string>();
    const locationByNormalizedName = new Map<string, string>();

    const officeByNormalizedName = new Map<string, string>();

    const resolveCompany = async (row: PeopleImportV1RowDto) => {
      const companyExternalId = (row.companyExternalId || '').trim();
      const companyName = (row.companyName || '').trim();
      const normalizedCompanyName = companyName ? normalizeName(companyName) : '';

      if (companyExternalId && companyByExternalId.has(companyExternalId)) {
        return companyByExternalId.get(companyExternalId) as string;
      }
      if (normalizedCompanyName && companyByNormalizedName.has(normalizedCompanyName)) {
        return companyByNormalizedName.get(normalizedCompanyName) as string;
      }

      let existing: any = null;
      if (companyExternalId) {
        existing = await companyModel.findOne({ orgId, externalId: companyExternalId, archivedAt: null }).lean();
      }
      if (!existing && normalizedCompanyName) {
        existing = await companyModel.findOne({ orgId, normalizedName: normalizedCompanyName, archivedAt: null }).lean();
      }

      if (existing?._id) {
        const id = String(existing._id);
        if (companyExternalId) companyByExternalId.set(companyExternalId, id);
        if (normalizedCompanyName) companyByNormalizedName.set(normalizedCompanyName, id);
        return id;
      }

      if (!companyName) {
        return null;
      }

      const created = await companyModel.create({
        orgId,
        name: companyName,
        normalizedName: normalizedCompanyName,
        externalId: companyExternalId || null,
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
      const companyId = String((created as any)._id || created.id);
      if (companyExternalId) companyByExternalId.set(companyExternalId, companyId);
      if (normalizedCompanyName) companyByNormalizedName.set(normalizedCompanyName, companyId);
      return companyId;
    };

    const resolveCompanyLocation = async (companyId: string, row: PeopleImportV1RowDto) => {
      const locationExternalId = (row.companyLocationExternalId || '').trim();
      const locationName = (row.companyLocationName || '').trim();
      const normalizedLocationName = locationName ? normalizeName(locationName) : '';

      if (locationExternalId && locationByExternalId.has(`${companyId}:${locationExternalId}`)) {
        return locationByExternalId.get(`${companyId}:${locationExternalId}`) as string;
      }
      if (normalizedLocationName && locationByNormalizedName.has(`${companyId}:${normalizedLocationName}`)) {
        return locationByNormalizedName.get(`${companyId}:${normalizedLocationName}`) as string;
      }

      let existing: any = null;
      if (locationExternalId) {
        existing = await companyLocationModel
          .findOne({ orgId, companyId, externalId: locationExternalId, archivedAt: null })
          .lean();
      }
      if (!existing && normalizedLocationName) {
        existing = await companyLocationModel
          .findOne({ orgId, companyId, normalizedName: normalizedLocationName, archivedAt: null })
          .lean();
      }

      if (existing?._id) {
        const id = String(existing._id);
        if (locationExternalId) locationByExternalId.set(`${companyId}:${locationExternalId}`, id);
        if (normalizedLocationName) locationByNormalizedName.set(`${companyId}:${normalizedLocationName}`, id);
        return id;
      }

      if (!locationName) {
        return null;
      }

      const created = await companyLocationModel.create({
        orgId,
        companyId,
        name: locationName,
        normalizedName: normalizedLocationName,
        externalId: locationExternalId || null,
        timezone: null,
        email: null,
        phone: null,
        addressLine1: null,
        addressLine2: null,
        city: null,
        region: null,
        postal: null,
        country: null,
        tagKeys: [],
        notes: null,
        archivedAt: null,
        piiStripped: false,
        legalHold: false,
      });
      const locationId = String((created as any)._id || created.id);
      if (locationExternalId) locationByExternalId.set(`${companyId}:${locationExternalId}`, locationId);
      if (normalizedLocationName) locationByNormalizedName.set(`${companyId}:${normalizedLocationName}`, locationId);
      return locationId;
    };

    const resolveOrgLocation = async (row: PeopleImportV1RowDto) => {
      const name = (row.orgLocationName || '').trim();
      if (!name) return null;
      const normalized = normalizeName(name);
      if (officeByNormalizedName.has(normalized)) return officeByNormalizedName.get(normalized) as string;
      const existing = await officeModel.findOne({ orgId, normalizedName: normalized, archivedAt: null }).lean();
      if (existing?._id) {
        const id = String(existing._id);
        officeByNormalizedName.set(normalized, id);
        return id;
      }
      const created = await officeModel.create({
        orgId,
        name,
        normalizedName: normalized,
        address: null,
        archivedAt: null,
        piiStripped: false,
        legalHold: false,
      });
      const id = String((created as any)._id || created.id);
      officeByNormalizedName.set(normalized, id);
      return id;
    };

    const results: Array<{ row: number; status: 'ok' | 'skipped' | 'error'; message?: string }> = [];
    let created = 0;
    let updated = 0;
    let skipped = 0;
    let invitesCreated = 0;
    let errors = 0;

    const withinFileDuplicateErrors = computeWithinFileDuplicateErrors(rows.filter((row) => row.action !== 'skip'));

    const personIdByRow = new Map<number, string>();
    const personDisplayByRow = new Map<number, string>();

    for (const row of rows) {
      if (row.action === 'skip') {
        skipped += 1;
        results.push({ row: row.row, status: 'skipped' });
        continue;
      }

      const duplicateErrors = withinFileDuplicateErrors.get(row.row);
      if (duplicateErrors?.length) {
        errors += 1;
        results.push({ row: row.row, status: 'error', message: duplicateErrors.join(' | ') });
        continue;
      }

      try {
        const { emails, primaryEmail } = this.derivePrimaryEmail(row.emails, row.primaryEmail);
        const phoneList = this.splitFreeTextList(row.phones);
        const tagKeys = this.splitFreeTextList(row.tagKeys);
        const skillKeys = this.splitFreeTextList(row.skillKeys);
        const certifications = this.splitFreeTextList(row.certifications);

        const companyId = await resolveCompany(row);
        const companyLocationId = companyId ? await resolveCompanyLocation(companyId, row) : null;
        const orgLocationId = await resolveOrgLocation(row);

        const personPayload: any = {
          personType: row.personType,
          displayName: row.displayName,
          title: row.title || undefined,
          departmentKey: row.departmentKey || undefined,
          emails: emails.length ? emails : undefined,
          primaryEmail,
          phones: phoneList.length ? phoneList : undefined,
          primaryPhone: row.primaryPhone || undefined,
          tagKeys: tagKeys.length ? tagKeys : undefined,
          skillKeys: skillKeys.length ? skillKeys : undefined,
          orgLocationId: orgLocationId || undefined,
          ironworkerNumber: row.ironworkerNumber || undefined,
          unionLocal: row.unionLocal || undefined,
          rating: typeof row.rating === 'number' ? row.rating : undefined,
          notes: row.notes || undefined,
          companyId: companyId || undefined,
          companyLocationId: companyLocationId || undefined,
        };

        if (certifications.length) {
          personPayload.certifications = certifications.map((name) => ({ name }));
        }

        let personId: string | null = null;

        if (row.action === 'create') {
          const createdPerson = await this.persons.create(actor as any, orgId, personPayload);
          personId = String((createdPerson as any)._id || (createdPerson as any).id);
          created += 1;
        } else if (row.action === 'update') {
          const targetId = row.personId;
          if (!targetId) throw new BadRequestException('personId is required for update rows');
          await this.persons.update(actor as any, orgId, targetId, personPayload);
          personId = String(targetId);
          updated += 1;
        }

        if (personId) {
          personIdByRow.set(row.row, personId);
          personDisplayByRow.set(row.row, row.displayName);
        }
        // Invites are intentionally not created during import; invites are managed explicitly via the Invites UI.

        results.push({ row: row.row, status: 'ok' });
      } catch (err: any) {
        errors += 1;
        results.push({ row: row.row, status: 'error', message: err?.message || 'import_failed' });
      }
    }

    const reportsToRows = rows.filter((r) => r.action !== 'skip' && (r.reportsToDisplayName || '').trim());
    if (reportsToRows.length) {
      const allDisplayNames = Array.from(personDisplayByRow.entries()).map(([rowNumber, name]) => ({
        rowNumber,
        normalized: normalizeName(name),
      }));
      const byNormalized = new Map<string, string>();
      allDisplayNames.forEach((entry) => {
        if (!byNormalized.has(entry.normalized)) {
          const personId = personIdByRow.get(entry.rowNumber);
          if (personId) byNormalized.set(entry.normalized, personId);
        }
      });

      const peopleModel = await this.personTenantModel(orgId);

      for (const row of reportsToRows) {
        const personId = personIdByRow.get(row.row);
        if (!personId) continue;
        if (row.personType !== 'internal_staff') continue;

        const managerName = (row.reportsToDisplayName || '').trim();
        const normalizedManager = normalizeName(managerName);
        let managerId = byNormalized.get(normalizedManager) || null;

        if (!managerId) {
          const rx = new RegExp(`^${this.escapeRegex(managerName)}$`, 'i');
          const matches = await peopleModel.find({ orgId, archivedAt: null, displayName: rx }).lean();
          if (matches.length === 1 && (matches[0] as any)._id) {
            managerId = String((matches[0] as any)._id);
          }
        }

        if (!managerId) {
          results.push({ row: row.row, status: 'ok', message: `reports_to unresolved: ${managerName}` });
          continue;
        }

        try {
          await this.persons.update(actor as any, orgId, personId, { reportsToPersonId: managerId } as any);
        } catch (err: any) {
          results.push({ row: row.row, status: 'ok', message: `reports_to update failed: ${err?.message || 'failed'}` });
        }
      }
    }

    await this.audit.logMutation({
      action: 'confirm_v1',
      entity: 'people_import_v1',
      orgId,
      userId: actor.userId,
      metadata: {
        processed: rows.length,
        created,
        updated,
        skipped,
        invitesCreated,
        errors,
      },
    });

    return {
      orgId,
      processed: rows.length,
      created,
      updated,
      skipped,
      invitesCreated,
      errors,
      results,
    };
  }
}
