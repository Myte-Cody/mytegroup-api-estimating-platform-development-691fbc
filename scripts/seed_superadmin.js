const dotenv = require('dotenv');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

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

  const email = process.env.SEED_SUPERADMIN_EMAIL || 'myte@mytegroup.com';
  const password = process.env.SEED_SUPERADMIN_PASSWORD || 'Myte@123Ahmed@123';

  let user = await User.findOne({ email }).lean();
  if (user) {
    console.log(`Super admin user already exists: ${email}`);
    await mongoose.disconnect();
    return;
  }

  const orgName = 'MYTE Root Org';
  let org = await Organization.findOne({ primaryDomain: 'mytegroup.com' }).lean();
  if (!org) {
    org = await Organization.create({ name: orgName, primaryDomain: 'mytegroup.com' });
  }

  const passwordHash = await bcrypt.hash(password, 10);
  user = await User.create({
    username: 'MYTE SuperAdmin',
    email,
    passwordHash,
    role: 'superadmin',
    roles: ['superadmin'],
    organizationId: org.id || org._id.toString(),
    isEmailVerified: true,
    isOrgOwner: true,
  });

  await Organization.updateOne(
    { _id: org.id || org._id },
    { $set: { ownerUserId: user.id || user._id.toString() } }
  );

  console.log('Seeded super admin user:', email);
  await mongoose.disconnect();
}

main().catch((err) => {
  console.error('Seed script failed', err);
  process.exit(1);
});

