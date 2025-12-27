/**
 * Phase 0.25 helper: backfill `orgId` from legacy `organizationId`, then remove `organizationId`.
 *
 * Usage (from backend repo):
 *   MONGO_URI="mongodb://localhost" MONGO_DB="MyteConstructionDev" node --require ts-node/register/transpile-only scripts/normalize_org_id.ts
 *
 * Optional:
 *   ORG_ID="..."           # only normalize a single org's dedicated DB (shared DB still scanned)
 *   DRY_RUN="1"            # print counts only; do not write
 *   COLLECTIONS="offices,projects,estimates,contacts,users"
 */
import { createConnection } from 'mongoose';
import { mongoConfig } from '../src/config/app.config';
import { TenantConnectionService } from '../src/common/tenancy/tenant-connection.service';
import { OrganizationSchema } from '../src/features/organizations/schemas/organization.schema';

type UpdateResult = {
  matched?: number;
  modified?: number;
};

type CollectionSummary = {
  name: string;
  backfill: UpdateResult;
  unset: UpdateResult;
};

const DEFAULT_COLLECTIONS = ['offices', 'projects', 'estimates', 'contacts', 'users'] as const;

function parseCollections(): string[] {
  const raw = (process.env.COLLECTIONS || '').trim();
  if (!raw) return [...DEFAULT_COLLECTIONS];
  return raw
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean);
}

async function normalizeCollection(conn: any, collectionName: string, dryRun: boolean): Promise<CollectionSummary> {
  const collection = conn.db.collection(collectionName);

  const backfillFilter = {
    $and: [
      { organizationId: { $type: 'string', $ne: '' } },
      {
        $or: [{ orgId: { $exists: false } }, { orgId: null }, { orgId: '' }],
      },
    ],
  };

  const unsetFilter = { organizationId: { $exists: true } };

  if (dryRun) {
    const backfillMatched = await collection.countDocuments(backfillFilter);
    const unsetMatched = await collection.countDocuments(unsetFilter);
    return {
      name: collectionName,
      backfill: { matched: backfillMatched, modified: 0 },
      unset: { matched: unsetMatched, modified: 0 },
    };
  }

  const backfillResult = await collection.updateMany(backfillFilter, [
    { $set: { orgId: '$organizationId' } },
  ]);
  const unsetResult = await collection.updateMany(unsetFilter, { $unset: { organizationId: '' } });

  return {
    name: collectionName,
    backfill: { matched: backfillResult.matchedCount, modified: backfillResult.modifiedCount },
    unset: { matched: unsetResult.matchedCount, modified: unsetResult.modifiedCount },
  };
}

async function main() {
  const dryRun = process.env.DRY_RUN === '1' || process.env.DRY_RUN === 'true';
  const orgIdFilter = (process.env.ORG_ID || '').trim() || null;
  const collections = parseCollections();

  const sharedConn = await createConnection(mongoConfig.uri, { dbName: mongoConfig.dbName }).asPromise();
  sharedConn.model('Organization', OrganizationSchema);
  const orgModel = sharedConn.model('Organization');
  const tenants = new TenantConnectionService(sharedConn as any, orgModel as any);

  const orgQuery: any = { archivedAt: null };
  if (orgIdFilter) orgQuery._id = orgIdFilter;

  const orgs: any[] = await orgModel.find(orgQuery).lean();

  const sharedSummaries: CollectionSummary[] = [];
  for (const name of collections) {
    sharedSummaries.push(await normalizeCollection(sharedConn, name, dryRun));
  }

  const dedicatedSummaries: Record<string, CollectionSummary[]> = {};
  for (const org of orgs) {
    const orgId = String(org._id || org.id);
    const conn = await tenants.getConnectionForOrg(orgId);
    if (conn === sharedConn) continue;

    dedicatedSummaries[orgId] = [];
    for (const name of collections.filter((c) => c !== 'users')) {
      dedicatedSummaries[orgId].push(await normalizeCollection(conn, name, dryRun));
    }

    await tenants.resetConnectionForOrg(orgId);
  }

  const format = (s: CollectionSummary) =>
    `${s.name}: backfill matched=${s.backfill.matched} modified=${s.backfill.modified}; unset matched=${s.unset.matched} modified=${s.unset.modified}`;

  // eslint-disable-next-line no-console
  console.log(`[orgId normalize] dryRun=${dryRun} sharedDb=${mongoConfig.dbName} uri=${mongoConfig.uri}`);
  // eslint-disable-next-line no-console
  console.log('[shared]');
  sharedSummaries.forEach((s) => console.log(`  - ${format(s)}`));

  const dedicatedIds = Object.keys(dedicatedSummaries);
  if (dedicatedIds.length) {
    // eslint-disable-next-line no-console
    console.log('[dedicated]');
    dedicatedIds.forEach((orgId) => {
      console.log(`  - orgId=${orgId}`);
      dedicatedSummaries[orgId].forEach((s) => console.log(`      ${format(s)}`));
    });
  }

  await sharedConn.close();
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(err);
  process.exit(1);
});

