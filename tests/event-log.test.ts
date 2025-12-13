require('reflect-metadata');
const { after, before, beforeEach, describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { Reflector } = require('@nestjs/core');
const { MongoMemoryServer } = require('mongodb-memory-server');
const mongoose = require('mongoose');
const { RolesGuard } = require('../src/common/guards/roles.guard');
const { EventLogSchema } = require('../src/common/events/event-log.schema');
const { EventLogService } = require('../src/common/events/event-log.service');
const { Role } = require('../src/common/roles');

describe('EventLogService', () => {
  let mongo;
  let connection;
  let model;
  let service;

  before(async () => {
    mongo = await MongoMemoryServer.create();
    connection = await mongoose.createConnection(mongo.getUri()).asPromise();
    model = connection.model('EventLog', EventLogSchema);
    service = new EventLogService(model);
  });

  after(async () => {
    await connection?.close();
    await mongo?.stop();
  });

  beforeEach(async () => {
    await model.deleteMany({});
  });

  const createEvent = (data = {}) =>
    model.create({
      eventType: data.eventType || 'test.event',
      entityType: data.entityType || 'Test',
      entityId: data.entityId || 'entity',
      orgId: data.orgId || 'org-1',
      action: data.action,
      actor: data.actor || 'user-1',
      createdAt: data.createdAt,
      archivedAt: data.archivedAt ?? null,
      piiStripped: data.piiStripped ?? false,
      legalHold: data.legalHold ?? false,
      metadata: data.metadata,
      payload: data.payload,
    });

  it('enforces org isolation when listing', async () => {
    await createEvent({ orgId: 'org-1', entityId: 'c1' });
    await createEvent({ orgId: 'org-2', entityId: 'c2' });
    const results = await service.list('org-1', {}, Role.Admin);
    assert.strictEqual(results.total, 1);
    assert.strictEqual(results.data.length, 1);
    assert.strictEqual(results.data[0].entityId, 'c1');
  });

  it('filters by action derived from eventType', async () => {
    await service.log({ eventType: 'project.created', orgId: 'org-1', entityType: 'Project', entityId: 'p1' });
    await service.log({ eventType: 'project.updated', orgId: 'org-1', entityType: 'Project', entityId: 'p2' });
    const filtered = await service.list('org-1', { action: 'updated' }, Role.Admin);
    assert.strictEqual(filtered.total, 1);
    assert.strictEqual(filtered.data.length, 1);
    assert.strictEqual(filtered.data[0].entityId, 'p2');
  });

  it('filters by eventType and returns wrapper with total', async () => {
    await service.log({ eventType: 'audit.login', orgId: 'org-1', entityType: 'Auth', entityId: 'u1' });
    await service.log({ eventType: 'audit.logout', orgId: 'org-1', entityType: 'Auth', entityId: 'u1' });
    const filtered = await service.list('org-1', { eventType: 'audit.login' }, Role.Admin);
    assert.strictEqual(filtered.total, 1);
    assert.strictEqual(filtered.data.length, 1);
    assert.strictEqual(filtered.data[0].eventType, 'audit.login');
  });

  it('applies pagination caps', async () => {
    const bulk = Array.from({ length: 120 }).map((_, idx) => ({
      eventType: `bulk.${idx}`,
      orgId: 'org-1',
      entityId: `bulk-${idx}`,
      createdAt: new Date(Date.now() + idx),
    }));
    await model.insertMany(bulk);
    const paged = await service.list('org-1', { limit: 200 }, Role.Admin);
    assert.strictEqual(paged.data.length, 100);
    assert.strictEqual(paged.total, 120);
    assert.ok(paged.nextCursor);
  });

  it('respects date range filters', async () => {
    await createEvent({ orgId: 'org-1', entityId: 'old', createdAt: new Date('2024-01-01') });
    await createEvent({ orgId: 'org-1', entityId: 'recent', createdAt: new Date('2024-05-01') });
    const filtered = await service.list(
      'org-1',
      { createdAtGte: new Date('2024-03-01'), createdAtLte: new Date('2024-06-01') },
      Role.Admin
    );
    assert.strictEqual(filtered.total, 1);
    assert.strictEqual(filtered.data.length, 1);
    assert.strictEqual(filtered.data[0].entityId, 'recent');
  });

  it('redacts payload/metadata when piiStripped is true', async () => {
    await createEvent({
      orgId: 'org-1',
      entityId: 'pii',
      piiStripped: true,
      metadata: { email: 'secret@example.com' },
      payload: { sensitive: true },
    });
    const results = await service.list('org-1', {}, Role.Admin);
    assert.strictEqual(results.data.length, 1);
    assert.strictEqual(results.data[0].metadata, undefined);
    assert.strictEqual(results.data[0].payload, undefined);
    assert.strictEqual(results.data[0].redacted, true);
  });

  it('redacts payload/metadata when legalHold is true', async () => {
    await createEvent({
      orgId: 'org-1',
      entityId: 'legal',
      legalHold: true,
      metadata: { email: 'legal@example.com' },
      payload: { sensitive: true },
    });
    const results = await service.list('org-1', {}, Role.Admin);
    assert.strictEqual(results.data.length, 1);
    assert.strictEqual(results.data[0].metadata, undefined);
    assert.strictEqual(results.data[0].payload, undefined);
    assert.strictEqual(results.data[0].redacted, true);
    assert.strictEqual(results.data[0].legalHold, true);
  });

  it('can retroactively apply compliance flags to entity events', async () => {
    await createEvent({
      orgId: 'org-1',
      entityType: 'Contact',
      entityId: 'contact-1',
      metadata: { email: 'secret@example.com' },
      payload: { sensitive: true },
    });

    await service.applyComplianceToEntityEvents({
      orgId: 'org-1',
      entity: 'Contact',
      entityId: 'contact-1',
      piiStripped: true,
    });

    const stored = await model.findOne({ orgId: 'org-1', entityId: 'contact-1' }).lean();
    assert(stored);
    assert.strictEqual(stored.piiStripped, true);
    assert.strictEqual(stored.metadata, undefined);
    assert.strictEqual(stored.payload, undefined);

    const results = await service.list('org-1', {}, Role.Admin);
    assert.strictEqual(results.data.length, 1);
    assert.strictEqual(results.data[0].redacted, true);
  });

  it('excludes archived events by default and includes them when requested', async () => {
    await createEvent({ orgId: 'org-1', entityId: 'active', archivedAt: null });
    await createEvent({ orgId: 'org-1', entityId: 'archived', archivedAt: new Date() });
    const defaults = await service.list('org-1', {}, Role.Admin);
    assert.ok(defaults.data.every((e) => e.entityId !== 'archived'));

    const withArchived = await service.list('org-1', { archived: true }, Role.Admin);
    const ids = withArchived.data.map((e) => e.entityId);
    assert.ok(ids.includes('archived'));
    assert.ok(ids.includes('active'));
  });

  it('supports cursor-based pagination', async () => {
    const baseTime = Date.now();
    await createEvent({ orgId: 'org-1', entityId: 'a', createdAt: new Date(baseTime) });
    await createEvent({ orgId: 'org-1', entityId: 'b', createdAt: new Date(baseTime + 1) });
    await createEvent({ orgId: 'org-1', entityId: 'c', createdAt: new Date(baseTime + 2) });

    const page1 = await service.list('org-1', { limit: 2 }, Role.Admin);
    assert.strictEqual(page1.data.length, 2);
    assert.strictEqual(page1.total, 3);
    assert.ok(page1.nextCursor);

    const page2 = await service.list('org-1', { cursor: page1.nextCursor, limit: 2 }, Role.Admin);
    assert.strictEqual(page2.data.length, 1);
    assert.strictEqual(page2.total, 3);
    assert.ok(!page2.nextCursor);
    assert.strictEqual(page2.data[0].entityId, 'a');
  });

  it('blocks org owner and non-privileged roles from reading audit logs', async () => {
    const ev = await createEvent({ orgId: 'org-1', entityId: 'secret' });
    await assert.rejects(() => service.list('org-1', {}, Role.OrgOwner), /Insufficient role/);
    await assert.rejects(() => service.findByIdScoped(ev.id, 'org-1', Role.OrgOwner), /Insufficient role/);
    await assert.rejects(() => service.list('org-1', {}, Role.Viewer), /Insufficient role/);
  });

  it('requires org scoping for super admins and honors explicit org override', async () => {
    await createEvent({ orgId: 'org-1', entityId: 'o1' });
    await createEvent({ orgId: 'org-2', entityId: 'o2' });

    await assert.rejects(() => service.list(undefined, {}, Role.SuperAdmin), /Organization context required/);

    const scoped = await service.list(undefined, { orgId: 'org-2' }, Role.SuperAdmin);
    assert.strictEqual(scoped.total, 1);
    assert.strictEqual(scoped.data[0].entityId, 'o2');
  });

  it('allows super admin to fetch an event by id when scoped to an org', async () => {
    const ev = await createEvent({ orgId: 'org-9', entityId: 'target' });
    const result = await service.findByIdScoped(ev.id, undefined, Role.SuperAdmin, 'org-9');
    assert.strictEqual(result.entityId, 'target');
  });

  it('blocks cross-tenant lookup when ids do not match org scope', async () => {
    const other = await createEvent({ orgId: 'org-2', entityId: 'secret' });
    await assert.rejects(() => service.findByIdScoped(other.id, 'org-1', Role.Admin), /Event not found/);
  });

  it('declares indexes for filter performance and retention with legal hold exemption', () => {
    const indexes = model.schema.indexes();
    const hasOrgCreated = indexes.some(([fields]) => fields.orgId === 1 && fields.createdAt === -1);
    const hasEntityIndex = indexes.some(([fields]) => fields.orgId === 1 && fields.entityId === 1 && fields.createdAt === -1);
    const hasActionIndex = indexes.some(([fields]) => fields.orgId === 1 && fields.action === 1 && fields.createdAt === -1);
    const hasEventTypeIndex = indexes.some(([fields]) => fields.orgId === 1 && fields.eventType === 1 && fields.createdAt === -1);
    const retention = indexes.find(([fields]) => fields.createdAt === 1 && !fields.orgId);

    assert.ok(hasOrgCreated, 'orgId + createdAt index is missing');
    assert.ok(hasEntityIndex, 'orgId + entityId index is missing');
    assert.ok(hasActionIndex, 'orgId + action index is missing');
    assert.ok(hasEventTypeIndex, 'orgId + eventType index is missing');
    assert.ok(retention, 'TTL index on createdAt is missing');

    const [, retentionOpts] = retention;
    assert.strictEqual(retentionOpts.expireAfterSeconds, 60 * 60 * 24 * 365 * 10);
    assert.deepStrictEqual(retentionOpts.partialFilterExpression, { legalHold: { $ne: true } });
  });
});

describe('RolesGuard for events endpoint', () => {
  const guard = new RolesGuard(new Reflector());
  const handler = () => undefined;
  const requiredRoles = [Role.Admin, Role.Compliance, Role.Security, Role.SuperAdmin];

  const ctxForRole = (role) =>
    ({
      switchToHttp: () => ({
        getRequest: () => ({ user: { role, orgId: 'org-1' }, session: { user: { role, orgId: 'org-1' } } }),
      }),
      getHandler: () => handler,
      getClass: () => ({}),
      getArgs: () => [],
      getArgByIndex: () => undefined,
      getType: () => 'http',
    } as any);

  it('denies non-privileged roles for audit log endpoints', () => {
    Reflect.defineMetadata('roles', requiredRoles, handler);
    assert.strictEqual(guard.canActivate(ctxForRole(Role.User)), false);
  });

  it('allows privileged roles', () => {
    Reflect.defineMetadata('roles', requiredRoles, handler);
    assert.strictEqual(guard.canActivate(ctxForRole(Role.Compliance)), true);
    assert.strictEqual(guard.canActivate(ctxForRole(Role.Security)), true);
    assert.strictEqual(guard.canActivate(ctxForRole(Role.SuperAdmin)), true);
  });
});
