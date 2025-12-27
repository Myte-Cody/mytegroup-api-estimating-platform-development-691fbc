/**
 * Repair script: link Person.userId for accepted invites where the linkage failed.
 *
 * Usage (from backend repo):
 *   MONGO_URI="mongodb://localhost" MONGO_DB="MyteConstructionDev" node --require ts-node/register/transpile-only scripts/repair_invite_person_links.ts
 *
 * Optional:
 *   ORG_ID="..."    # only repair a single org
 *   DRY_RUN="1"     # report only, no writes
 *   LIMIT="500"     # cap number of invites scanned
 */
import { createConnection } from 'mongoose';
import { mongoConfig } from '../src/config/app.config';
import { TenantConnectionService } from '../src/common/tenancy/tenant-connection.service';
import { normalizeEmail } from '../src/common/utils/normalize.util';
import { OrganizationSchema } from '../src/features/organizations/schemas/organization.schema';
import { InviteSchema } from '../src/features/invites/schemas/invite.schema';
import { PersonSchema } from '../src/features/persons/schemas/person.schema';

type InviteRecord = {
  _id?: string;
  id?: string;
  orgId?: string;
  personId?: string | null;
  email?: string;
  invitedUserId?: string | null;
  status?: string;
  archivedAt?: Date | null;
};

const parseBoolean = (value: string | undefined) => value === '1' || value === 'true';

async function main() {
  const dryRun = parseBoolean(process.env.DRY_RUN);
  const orgIdFilter = (process.env.ORG_ID || '').trim() || null;
  const limit = Number(process.env.LIMIT || 0) || 0;

  const sharedConn = await createConnection(mongoConfig.uri, { dbName: mongoConfig.dbName }).asPromise();
  sharedConn.model('Organization', OrganizationSchema);
  const orgModel = sharedConn.model('Organization');
  const inviteModel = sharedConn.model('Invite', InviteSchema);
  const basePersonModel = sharedConn.model('Person', PersonSchema);
  const tenants = new TenantConnectionService(sharedConn as any, orgModel as any);

  const query: Record<string, any> = {
    status: 'accepted',
    archivedAt: null,
    orgId: { $exists: true, $ne: null },
    invitedUserId: { $exists: true, $ne: null },
  };
  if (orgIdFilter) query.orgId = orgIdFilter;

  const invites: InviteRecord[] = await inviteModel
    .find(query)
    .limit(limit || 0)
    .lean();

  let scanned = 0;
  let linked = 0;
  let skipped = 0;
  let missingPerson = 0;
  let mismatchedUser = 0;
  const touchedOrgs = new Set<string>();

  for (const invite of invites) {
    scanned += 1;
    const orgId = String(invite.orgId || '').trim();
    const invitedUserId = String(invite.invitedUserId || '').trim();
    if (!orgId || !invitedUserId) {
      skipped += 1;
      continue;
    }

    touchedOrgs.add(orgId);
    const peopleModel = await tenants.getModelForOrg(orgId, 'Person', PersonSchema, basePersonModel as any);

    const personId = (invite.personId || '').trim();
    let person: any = null;
    if (personId) {
      person = await peopleModel.findOne({ _id: personId, orgId }).lean();
    }

    if (!person) {
      const email = normalizeEmail(invite.email || '');
      if (email) {
        person = await peopleModel
          .findOne({
            orgId,
            archivedAt: null,
            $or: [{ primaryEmail: email }, { 'emails.normalized': email }, { 'emails.value': email }],
          })
          .lean();
        if (!person) {
          person = await peopleModel.findOne({
            orgId,
            $or: [{ primaryEmail: email }, { 'emails.normalized': email }, { 'emails.value': email }],
          }).lean();
        }
      }
    }

    if (!person) {
      missingPerson += 1;
      continue;
    }

    const currentUserId = person.userId ? String(person.userId) : '';
    if (currentUserId && currentUserId !== invitedUserId) {
      mismatchedUser += 1;
      continue;
    }

    if (currentUserId === invitedUserId) {
      skipped += 1;
      continue;
    }

    if (dryRun) {
      linked += 1;
      continue;
    }

    await peopleModel.updateOne(
      { _id: person._id, orgId },
      { $set: { userId: invitedUserId, updatedAt: new Date() } }
    );
    linked += 1;
  }

  for (const orgId of touchedOrgs) {
    await tenants.resetConnectionForOrg(orgId);
  }

  await sharedConn.close();

  // eslint-disable-next-line no-console
  console.log(
    `[repair invite links] dryRun=${dryRun} scanned=${scanned} linked=${linked} skipped=${skipped} missingPerson=${missingPerson} mismatchedUser=${mismatchedUser}`
  );
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(err);
  process.exit(1);
});
