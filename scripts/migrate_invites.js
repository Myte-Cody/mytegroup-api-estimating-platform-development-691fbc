/**
 * Invite migration helper: move invites for a single org between Mongo databases.
 *
 * Usage:
 *   MONGO_SOURCE_URI="mongodb://localhost:27017" \
 *   MONGO_SOURCE_DB="MyteConstructionDev" \
 *   MONGO_TARGET_URI="mongodb://otherhost:27017" \
 *   MONGO_TARGET_DB="OrgSpecificDb" \
 *   ORG_ID="..." \
 *   node scripts/migrate_invites.js
 *
 * Notes:
 * - This copies invites for the given org from source to target.
 * - It does NOT drop target data; reruns may duplicate records if IDs differ.
 * - Ensure you run during a maintenance window; consider pausing invite creation.
 */
const mongoose = require('mongoose');

const InviteSchema = new mongoose.Schema(
  {
    orgId: { type: String, required: true, index: true },
    email: { type: String, required: true },
    role: { type: String, required: true },
    contactId: { type: String, default: null },
    tokenHash: { type: String, required: true },
    tokenExpires: { type: Date, required: true },
    status: { type: String, enum: ['pending', 'accepted', 'expired'], default: 'pending' },
    createdByUserId: { type: String, required: true },
    invitedUserId: { type: String, default: null },
    acceptedAt: { type: Date, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

async function main() {
  const orgId = process.env.ORG_ID;
  if (!orgId) throw new Error('ORG_ID is required');
  const sourceUri = process.env.MONGO_SOURCE_URI || 'mongodb://localhost:27017';
  const sourceDb = process.env.MONGO_SOURCE_DB || 'MyteConstructionDev';
  const targetUri = process.env.MONGO_TARGET_URI;
  const targetDb = process.env.MONGO_TARGET_DB;
  if (!targetUri || !targetDb) throw new Error('MONGO_TARGET_URI and MONGO_TARGET_DB are required');

  const source = await mongoose.createConnection(sourceUri, { dbName: sourceDb }).asPromise();
  const target = await mongoose.createConnection(targetUri, { dbName: targetDb }).asPromise();

  const SourceInvite = source.model('Invite', InviteSchema);
  const TargetInvite = target.model('Invite', InviteSchema);

  const docs = await SourceInvite.find({ orgId }).lean();
  if (!docs.length) {
    console.log('No invites found for org', orgId);
    await source.close();
    await target.close();
    return;
  }

  console.log(`Copying ${docs.length} invites for org ${orgId}`);
  for (const doc of docs) {
    const { _id, ...rest } = doc;
    await TargetInvite.updateOne({ _id }, { $set: rest }, { upsert: true });
  }
  console.log('Done.');
  await source.close();
  await target.close();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
