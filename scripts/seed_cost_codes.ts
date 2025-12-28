import 'dotenv/config';
import mongoose, { Connection } from 'mongoose';
import { mongoConfig } from '../src/config/app.config';
import { OrganizationSchema } from '../src/features/organizations/schemas/organization.schema';
import { CostCodeSchema } from '../src/features/cost-codes/schemas/cost-code.schema';
import { STEEL_FIELD_PACK } from '../src/features/cost-codes/seed/steel-field-pack';

const args = process.argv.slice(2);
const getArg = (key: string) => {
  const idx = args.indexOf(`--${key}`);
  if (idx === -1) return undefined;
  const val = args[idx + 1];
  if (!val || val.startsWith('--')) return undefined;
  return val;
};
const hasFlag = (key: string) => args.includes(`--${key}`);

const orgIdArg = getArg('orgId') || getArg('org-id');
const orgNameArg = getArg('orgName') || getArg('org-name');
const replace = hasFlag('replace');
const allowUsed = hasFlag('allow-used');

const defaultOrgName = (process.env.DEV_SEED_ORG_NAME || 'Myte Group Inc.').trim();

const run = async () => {
  await mongoose.connect(mongoConfig.uri, { dbName: mongoConfig.dbName });
  const Organization = mongoose.model('Organization', OrganizationSchema);

  let org: any = null;
  if (orgIdArg) {
    org = await Organization.findById(orgIdArg);
  } else {
    const name = orgNameArg || defaultOrgName;
    org = await Organization.findOne({ name });
  }

  if (!org) {
    throw new Error('Organization not found. Provide --orgId or --orgName.');
  }

  const orgId = String(org.id || org._id);
  let connection: Connection = mongoose.connection;
  let dedicatedConn: Connection | null = null;

  if (org.useDedicatedDb && org.databaseUri) {
    dedicatedConn = await mongoose
      .createConnection(org.databaseUri, { dbName: org.databaseName || undefined })
      .asPromise();
    connection = dedicatedConn;
  }

  const CostCode = connection.model('CostCode', CostCodeSchema);

  const existingCount = await CostCode.countDocuments({ orgId });
  const usedCount = await CostCode.countDocuments({ orgId, isUsed: true });

  if (usedCount > 0 && !allowUsed) {
    throw new Error(`Seed aborted: ${usedCount} cost code(s) already in use.`);
  }

  if (existingCount > 0 && !replace) {
    console.log(`Seed skipped: ${existingCount} cost codes already exist for org ${orgId}.`);
    if (dedicatedConn) await dedicatedConn.close();
    await mongoose.disconnect();
    return;
  }

  if (replace) {
    await CostCode.deleteMany({ orgId });
  }

  const toInsert = STEEL_FIELD_PACK.map((code) => ({
    orgId,
    category: code.category,
    code: code.code,
    description: code.description,
    active: code.active ?? false,
    isUsed: code.isUsed ?? false,
    deactivatedAt: null,
  }));

  const inserted = await CostCode.insertMany(toInsert);
  console.log(`Seeded ${inserted.length} cost codes for org ${orgId}.`);

  if (dedicatedConn) await dedicatedConn.close();
  await mongoose.disconnect();
};

run().catch((err) => {
  console.error(err);
  process.exitCode = 1;
  mongoose.disconnect().catch(() => null);
});
