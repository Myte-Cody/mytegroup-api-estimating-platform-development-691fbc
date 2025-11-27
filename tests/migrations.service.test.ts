const { describe, it, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const { MongoMemoryServer } = require('mongodb-memory-server');
const { Connection, Model, createConnection } = require('mongoose');
const { EventLogSchema } = require('../src/common/events/event-log.schema');
const { EventLogService } = require('../src/common/events/event-log.service');
const { AuditLogService } = require('../src/common/services/audit-log.service');
const { Role } = require('../src/common/roles');
const { ContactSchema } = require('../src/features/contacts/schemas/contact.schema');
const { InviteSchema } = require('../src/features/invites/schemas/invite.schema');
const { MigrationsService } = require('../src/features/migrations/migrations.service');
const { TenantMigrationSchema } = require('../src/features/migrations/schemas/tenant-migration.schema');
const { OfficeSchema } = require('../src/features/offices/schemas/office.schema');
const { Organization, OrganizationSchema } = require('../src/features/organizations/schemas/organization.schema');
const { ProjectSchema } = require('../src/features/projects/schemas/project.schema');
const { UserSchema } = require('../src/features/users/schemas/user.schema');

const ACTOR = { id: 'super-1', role: Role.SuperAdmin };
const SHARED_DB = 'sharedDb';
const DEDICATED_DB = 'tenantDb';

describe('MigrationsService', () => {
  let sharedServer: MongoMemoryServer;
  let dedicatedServer: MongoMemoryServer;
  let sharedConn: Connection;
  let dedicatedSeedConn: Connection | undefined;
  let migrations: MigrationsService;
  let orgModel: Model<Organization>;

  beforeEach(async () => {
    sharedServer = await MongoMemoryServer.create();
    dedicatedServer = await MongoMemoryServer.create();
    sharedConn = await createConnection(sharedServer.getUri(), { dbName: SHARED_DB }).asPromise();

    // Register schemas on shared connection
    sharedConn.model('Organization', OrganizationSchema);
    sharedConn.model('User', UserSchema);
    sharedConn.model('Contact', ContactSchema);
    sharedConn.model('Invite', InviteSchema);
    sharedConn.model('Project', ProjectSchema);
    sharedConn.model('Office', OfficeSchema);
    const eventLogModel = sharedConn.model('EventLog', EventLogSchema);
    const migrationModel = sharedConn.model('TenantMigration', TenantMigrationSchema);

    const audit = new AuditLogService(new EventLogService(eventLogModel as any));
    orgModel = sharedConn.model('Organization');
    migrations = new MigrationsService(sharedConn, migrationModel as any, orgModel as any, audit);
  });

  afterEach(async () => {
    const cache: Map<string, Promise<Connection>> | undefined = (migrations as any)?.connectionCache;
    if (cache) {
      for (const connPromise of cache.values()) {
        const conn = await connPromise;
        if (conn.readyState === 1) {
          await conn.close();
        }
      }
      cache.clear();
    }
    if (dedicatedSeedConn) {
      await dedicatedSeedConn.close();
      dedicatedSeedConn = undefined;
    }
    if (sharedConn) await sharedConn.close();
    await sharedServer.stop();
    await dedicatedServer.stop();
  });

  const seedSharedOrgData = async (opts?: { legalHoldOrg?: boolean }) => {
    const org = await orgModel.create({
      name: 'Acme Co',
      useDedicatedDb: false,
      databaseUri: dedicatedServer.getUri(),
      databaseName: DEDICATED_DB,
      dataResidency: 'shared',
      legalHold: opts?.legalHoldOrg || false,
      piiStripped: false,
    });

    const userModel = sharedConn.model('User');
    const contactModel = sharedConn.model('Contact');
    const inviteModel = sharedConn.model('Invite');
    const projectModel = sharedConn.model('Project');
    const officeModel = sharedConn.model('Office');
    const eventLogModel = sharedConn.model('EventLog');

    const actorUser = await userModel.create({
      username: 'owner',
      email: 'owner@example.com',
      passwordHash: 'hash',
      role: Role.Admin,
      roles: [Role.Admin],
      organizationId: org.id,
      piiStripped: false,
      legalHold: false,
    });

    await contactModel.create({
      orgId: org.id,
      name: 'Contact A',
      email: 'contact@example.com',
      roles: [],
      tags: [],
      piiStripped: true,
      legalHold: true,
    });

    await inviteModel.create({
      orgId: org.id,
      email: 'invitee@example.com',
      role: Role.User,
      tokenHash: 'hash',
      tokenExpires: new Date(Date.now() + 60 * 60 * 1000),
      status: 'pending',
      createdByUserId: actorUser.id,
      piiStripped: false,
      legalHold: false,
    });

    await projectModel.create({
      name: 'Project A',
      organizationId: org.id,
      description: 'test project',
      piiStripped: false,
      legalHold: false,
    });

    await officeModel.create({
      name: 'HQ',
      organizationId: org.id,
      address: '123 Main',
      piiStripped: false,
      legalHold: false,
    });

    await eventLogModel.create({
      eventType: 'seed',
      orgId: org.id,
      metadata: { note: 'seed data' },
    });

    return org;
  };

  const seedDedicatedOrgData = async () => {
    const org = await orgModel.create({
      name: 'Beta Co',
      useDedicatedDb: true,
      databaseUri: dedicatedServer.getUri(),
      databaseName: DEDICATED_DB,
      dataResidency: 'dedicated',
      legalHold: false,
      piiStripped: false,
    });
    dedicatedSeedConn = await createConnection(dedicatedServer.getUri(), { dbName: DEDICATED_DB }).asPromise();
    dedicatedSeedConn.model('User', UserSchema);
    dedicatedSeedConn.model('Contact', ContactSchema);
    dedicatedSeedConn.model('Invite', InviteSchema);
    dedicatedSeedConn.model('Project', ProjectSchema);
    dedicatedSeedConn.model('Office', OfficeSchema);
    dedicatedSeedConn.model('EventLog', EventLogSchema);

    await dedicatedSeedConn.model('User').create({
      username: 'dedicatedUser',
      email: 'dedicated@example.com',
      passwordHash: 'hash',
      role: Role.Admin,
      roles: [Role.Admin],
      organizationId: org.id,
      piiStripped: false,
      legalHold: false,
    });

    await dedicatedSeedConn.model('Contact').create({
      orgId: org.id,
      name: 'Dedicated Contact',
      roles: [],
      tags: [],
      piiStripped: false,
      legalHold: false,
    });

    await dedicatedSeedConn.model('EventLog').create({
      eventType: 'seed.dedicated',
      orgId: org.id,
    });

    return org;
  };

  it('migrates shared data to dedicated with compliance fields and progress', async () => {
    const org = await seedSharedOrgData();

    const migration = await migrations.start(
      {
        orgId: org.id,
        direction: 'shared_to_dedicated',
        targetUri: dedicatedServer.getUri(),
        targetDbName: DEDICATED_DB,
        chunkSize: 10,
      },
      ACTOR
    );

    assert.strictEqual(migration.status, 'ready_for_cutover');
    assert.ok(migration.progress.contacts.total >= 1);
    assert.strictEqual(migration.progress.contacts.copied, migration.progress.contacts.total);

    const targetConn = await createConnection(dedicatedServer.getUri(), { dbName: DEDICATED_DB }).asPromise();
    const migratedContact = await targetConn.model('Contact', ContactSchema).findOne({ orgId: org.id }).lean();
    assert.ok(migratedContact, 'expected contact to be migrated');
    assert.strictEqual(migratedContact?.legalHold, true, 'expected legalHold to be preserved');
    assert.strictEqual(migratedContact?.piiStripped, true, 'expected piiStripped to be preserved');

    const migratedInvite = await targetConn.model('Invite', InviteSchema).findOne({ orgId: org.id }).lean();
    assert.ok(migratedInvite, 'expected invite to be migrated');

    await migrations.finalize(
      { migrationId: migration._id.toString(), orgId: org.id, confirmCutover: true },
      ACTOR
    );
    const updatedOrg = await orgModel.findById(org.id);
    assert.strictEqual(updatedOrg?.useDedicatedDb, true);
    assert.strictEqual(updatedOrg?.dataResidency, 'dedicated');

    await targetConn.close();
  });

  it('blocks migrations under legal hold unless override is provided', async () => {
    const org = await seedSharedOrgData({ legalHoldOrg: true });
    await assert.rejects(
      () =>
        migrations.start(
          {
            orgId: org.id,
            direction: 'shared_to_dedicated',
            targetUri: dedicatedServer.getUri(),
            targetDbName: DEDICATED_DB,
          },
          ACTOR
        ),
      /legal hold/i
    );

    const migration = await migrations.start(
      {
        orgId: org.id,
        direction: 'shared_to_dedicated',
        targetUri: dedicatedServer.getUri(),
        targetDbName: DEDICATED_DB,
        overrideLegalHold: true,
      },
      ACTOR
    );
    assert.strictEqual(migration.status, 'ready_for_cutover');
  });

  it('aborts migrations and cleans target data', async () => {
    const org = await seedSharedOrgData();
    const migration = await migrations.start(
      {
        orgId: org.id,
        direction: 'shared_to_dedicated',
        targetUri: dedicatedServer.getUri(),
        targetDbName: DEDICATED_DB,
      },
      ACTOR
    );

    const targetConn = await createConnection(dedicatedServer.getUri(), { dbName: DEDICATED_DB }).asPromise();
    assert.ok(await targetConn.model('Contact', ContactSchema).countDocuments({ orgId: org.id }));

    const aborted = await migrations.abort(
      { migrationId: migration._id.toString(), orgId: org.id, reason: 'test abort' },
      ACTOR
    );
    assert.strictEqual(aborted.status, 'aborted');

    const remaining = await targetConn.model('Contact', ContactSchema).countDocuments({ orgId: org.id });
    assert.strictEqual(remaining, 0);
    await targetConn.close();
  });

  it('rejects non-superadmin actors', async () => {
    const org = await seedSharedOrgData();
    await assert.rejects(
      () =>
        migrations.start(
          {
            orgId: org.id,
            direction: 'shared_to_dedicated',
            targetUri: dedicatedServer.getUri(),
            targetDbName: DEDICATED_DB,
          },
          { id: 'user-1', role: Role.Admin }
        ),
      /restricted/
    );
  });

  it('migrates dedicated data back to shared when requested', async () => {
    const org = await seedDedicatedOrgData();

    const migration = await migrations.start(
      {
        orgId: org.id,
        direction: 'dedicated_to_shared',
        targetUri: sharedServer.getUri(),
        targetDbName: SHARED_DB,
      },
      ACTOR
    );

    assert.strictEqual(migration.status, 'ready_for_cutover');
    const copied = await sharedConn.model('User').countDocuments({ organizationId: org.id });
    assert.strictEqual(copied, 1);

    await migrations.finalize(
      { migrationId: migration._id.toString(), orgId: org.id, confirmCutover: true },
      ACTOR
    );
    const updatedOrg = await orgModel.findById(org.id).lean();
    assert.strictEqual(updatedOrg?.useDedicatedDb, false);
    assert.strictEqual(updatedOrg?.dataResidency, 'shared');
  });

  it('rejects finalize when migration is dry-run or not ready', async () => {
    const org = await orgModel.create({
      name: 'Gamma',
      useDedicatedDb: false,
      databaseUri: sharedServer.getUri(),
      databaseName: SHARED_DB,
      dataResidency: 'shared',
      legalHold: false,
      piiStripped: false,
    });

    const dryRunMig = await sharedConn.model('TenantMigration').create({
      orgId: org.id,
      direction: 'shared_to_dedicated',
      status: 'ready_for_cutover',
      dryRun: true,
    });

    await assert.rejects(
      () => migrations.finalize({ migrationId: dryRunMig.id, orgId: org.id, confirmCutover: true }, ACTOR),
      /dry-run/i
    );

    const notReadyMig = await sharedConn.model('TenantMigration').create({
      orgId: org.id,
      direction: 'shared_to_dedicated',
      status: 'in_progress',
      dryRun: false,
    });

    await assert.rejects(
      () => migrations.finalize({ migrationId: notReadyMig.id, orgId: org.id, confirmCutover: true }, ACTOR),
      /not ready/i
    );
  });
});
