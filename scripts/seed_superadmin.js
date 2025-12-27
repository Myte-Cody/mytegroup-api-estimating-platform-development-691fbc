const dotenv = require('dotenv');
const mongoose = require('mongoose');
const argon2 = require('argon2');

dotenv.config();

const { Schema } = mongoose;

const OrganizationSchema = new Schema({
  name: { type: String, required: true, unique: true },
  metadata: { type: Schema.Types.Mixed, default: {} },
  ownerUserId: { type: String, default: null },
  createdByUserId: { type: String, default: null },
  primaryDomain: { type: String, default: null, index: true, unique: true, sparse: true },
  useDedicatedDb: { type: Boolean, default: false },
  datastoreType: { type: String, enum: ['shared', 'dedicated'], default: 'shared' },
  databaseUri: { type: String, default: null },
  databaseName: { type: String, default: null },
  dataResidency: { type: String, enum: ['shared', 'dedicated'], default: 'shared' },
  lastMigratedAt: { type: Date, default: null },
  archivedAt: { type: Date, default: null },
  piiStripped: { type: Boolean, default: false },
  legalHold: { type: Boolean, default: false },
  datastoreHistory: [
    {
      fromType: { type: String },
      toType: { type: String },
      fromUri: { type: String, default: null },
      toUri: { type: String, default: null },
      actorId: { type: String, default: null },
      switchedAt: { type: Date, default: Date.now },
    },
  ],
}, { timestamps: true });

const UserSchema = new Schema({
  username: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  passwordHash: { type: String, required: true },
  role: { type: String, default: 'user' },
  roles: { type: [String], default: ['user'] },
  organizationId: { type: String },
  isEmailVerified: { type: Boolean, default: false },
  verificationTokenHash: { type: String, default: null },
  verificationTokenExpires: { type: Date, default: null },
  resetTokenHash: { type: String, default: null },
  resetTokenExpires: { type: Date, default: null },
  archivedAt: { type: Date, default: null },
  lastLogin: { type: Date, default: null },
  isOrgOwner: { type: Boolean, default: false },
  piiStripped: { type: Boolean, default: false },
  legalHold: { type: Boolean, default: false },
}, { timestamps: true });

async function main() {
  const uri = process.env.MONGO_URI;
  const dbName = process.env.MONGO_DB || 'MyteConstructionDev';
  if (!uri) {
    console.error('MONGO_URI is not set');
    process.exit(1);
  }

  await mongoose.connect(uri, { dbName });
  const Organization = mongoose.model('Organization', OrganizationSchema);
  const User = mongoose.model('User', UserSchema);

  const email = (process.env.SEED_SUPERADMIN_EMAIL || '').trim().toLowerCase();
  const password = (process.env.SEED_SUPERADMIN_PASSWORD || '').trim();
  const primaryDomain = (process.env.SEED_SUPERADMIN_PRIMARY_DOMAIN || 'mytegroup.com').trim().toLowerCase();
  const orgName = (process.env.SEED_SUPERADMIN_ORG_NAME || 'Myte Group Inc').trim();
  const role = (process.env.SEED_SUPERADMIN_ROLE || 'platform_admin').trim();
  const canonicalSeedEmail = 'ahmed.mekallach@mytegroup.com';

  if (!email) {
    console.error('SEED_SUPERADMIN_EMAIL is required');
    process.exit(1);
  }
  if (email !== canonicalSeedEmail) {
    console.error(`Only ${canonicalSeedEmail} is allowed for platform admin seeding`);
    process.exit(1);
  }
  if (!password) {
    console.error('SEED_SUPERADMIN_PASSWORD is required');
    process.exit(1);
  }
  if (!primaryDomain) {
    console.error('SEED_SUPERADMIN_PRIMARY_DOMAIN is required');
    process.exit(1);
  }

  const org = await Organization.findOneAndUpdate(
    { primaryDomain },
    { $setOnInsert: { primaryDomain }, $set: { name: orgName } },
    { new: true, upsert: true }
  ).lean();
  const orgId = org.id || org._id.toString();

  const existing = await User.findOne({ email }).lean();
  const passwordHash = await argon2.hash(password, { type: argon2.argon2id });
  const user = await User.findOneAndUpdate(
    { email },
    {
      $setOnInsert: { email },
      $set: {
        username: existing?.username || 'MYTE SuperAdmin',
        passwordHash,
        role,
        roles: [role],
        organizationId: orgId,
        isEmailVerified: true,
        isOrgOwner: true,
        archivedAt: null,
        legalHold: false,
      },
    },
    { new: true, upsert: true }
  ).lean();

  await Organization.updateOne({ _id: orgId }, { $set: { ownerUserId: user.id || user._id.toString() } });

  console.log('Seeded super admin user:', email);
  await mongoose.disconnect();
}

main().catch((err) => {
  console.error('Seed script failed', err);
  process.exit(1);
});
