const { ForbiddenException } = require('@nestjs/common');
const assert = require('node:assert/strict');
const { beforeEach, describe, it } = require('node:test');
const { ComplianceService } = require('../src/features/compliance/compliance.service');
const { Role } = require('../src/common/roles');

const createModel = (records = []) => {
  const store = records.map((rec) => ({ ...rec }));
  const toDoc = (record) => {
    const doc = { ...record };
    doc.save = async () => {
      const idx = store.findIndex((entry) => entry._id === record._id || entry.id === record.id);
      if (idx >= 0) {
        store[idx] = { ...store[idx], ...doc };
        Object.assign(doc, store[idx]);
      }
      return doc;
    };
    doc.toObject = () => {
      const { save, toObject, ...rest } = doc;
      return { ...rest };
    };
    return doc;
  };
  return {
    records: store,
    async findById(id) {
      const match = store.find((rec) => rec._id === id || rec.id === id);
      return match ? toDoc(match) : null;
    },
  };
};

const buildService = () => {
  const userModel = createModel([
    {
      _id: 'user-1',
      email: 'user@example.com',
      username: 'User One',
      passwordHash: 'hash',
      role: Role.User,
      organizationId: 'org-1',
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
      lastLogin: new Date('2024-01-01T00:00:00Z'),
    },
  ]);
  const contactModel = createModel([
    {
      _id: 'contact-1',
      orgId: 'org-1',
      name: 'Alice Contact',
      email: 'alice@example.com',
      phone: '123',
      notes: 'Sensitive notes',
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    },
    {
      _id: 'contact-legal',
      orgId: 'org-1',
      name: 'On Hold',
      email: 'hold@example.com',
      legalHold: true,
      archivedAt: null,
      piiStripped: false,
    },
  ]);
  const inviteModel = createModel([
    {
      _id: 'invite-1',
      orgId: 'org-1',
      email: 'invitee@example.com',
      role: Role.User,
      tokenHash: 'token-hash',
      tokenExpires: new Date(Date.now() + 60 * 60 * 1000),
      status: 'pending',
      createdByUserId: 'actor-1',
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    },
    {
      _id: 'invite-hold',
      orgId: 'org-1',
      email: 'legalhold@example.com',
      role: Role.User,
      tokenHash: 'token-hash',
      tokenExpires: new Date(Date.now() + 60 * 60 * 1000),
      status: 'pending',
      createdByUserId: 'actor-1',
      archivedAt: null,
      piiStripped: false,
      legalHold: true,
    },
  ]);
  const projectModel = createModel([
    {
      _id: 'project-1',
      name: 'Tower Project',
      description: 'Client address in description',
      organizationId: 'org-1',
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    },
  ]);
  const estimateModel = createModel([
    {
      _id: 'estimate-1',
      projectId: 'project-1',
      organizationId: 'org-1',
      createdByUserId: 'actor-1',
      name: 'Base Estimate',
      description: 'Sensitive estimate description',
      notes: 'Notes',
      status: 'draft',
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    },
  ]);
  const auditLog = [];
  const audit = { log: async (event) => auditLog.push(event) };
  const eventsCalls = [];
  const events = { applyComplianceToEntityEvents: async (params) => eventsCalls.push(params) };
  const tenants = { getModelForOrg: async () => contactModel };
  const service = new ComplianceService(
    userModel,
    contactModel,
    inviteModel,
    projectModel,
    estimateModel,
    audit,
    events,
    tenants
  );
  return { service, auditLog, eventsCalls, userModel, contactModel, inviteModel, projectModel, estimateModel };
};

describe('ComplianceService', () => {
  let ctx;
  const actor = { id: 'actor-1', orgId: 'org-1', role: Role.Admin };

  beforeEach(() => {
    ctx = buildService();
  });

  it('redacts user PII, preserves required fields, and logs without leaking values', async () => {
    await ctx.service.stripPii({ entityType: 'user', entityId: 'user-1' }, actor);

    const user = ctx.userModel.records.find((u) => u._id === 'user-1');
    assert(user);
    assert(user.piiStripped);
    assert.match(user.email, /^redacted\+/);
    assert.match(user.username, /^redacted-user-/);
    assert.equal(user.lastLogin, null);

    const event = ctx.auditLog.find((e) => e.eventType === 'compliance.strip_pii');
    assert(event);
    assert.deepEqual(event.metadata.redactedFields, ['email', 'username', 'lastLogin']);
    assert.equal(event.metadata.before.email, '<redacted>');
    assert(!event.metadata.before.email.includes('user@example.com'));
    assert.ok(ctx.eventsCalls.some((call) => call.entityId === 'user-1' && call.piiStripped === true));
  });

  it('strips invite PII, expires token, and emits audit snapshot', async () => {
    await ctx.service.stripPii({ entityType: 'invite', entityId: 'invite-1' }, actor);

    const invite = ctx.inviteModel.records.find((i) => i._id === 'invite-1');
    assert(invite);
    assert(invite.piiStripped);
    assert.equal(invite.status, 'expired');
    assert(invite.tokenExpires instanceof Date);
    assert.equal(invite.tokenExpires?.getTime(), 0);

    const event = ctx.auditLog.find((e) => e.eventType === 'compliance.strip_pii');
    assert(event);
    assert.deepEqual(event.metadata.redactedFields, ['email', 'tokenHash', 'tokenExpires']);
    assert.equal(event.metadata.before.tokenExpires, '<redacted>');
    assert.equal(event.metadata.after.tokenExpires?.getTime?.(), 0);
    assert.ok(ctx.eventsCalls.some((call) => call.entityId === 'invite-1' && call.piiStripped === true));
  });

  it('blocks strip when entity is on legal hold', async () => {
    await assert.rejects(
      () => ctx.service.stripPii({ entityType: 'contact', entityId: 'contact-legal' }, actor),
      (err) => err instanceof ForbiddenException
    );
    const contact = ctx.contactModel.records.find((c) => c._id === 'contact-legal');
    assert(contact);
    assert(!contact.piiStripped);
  });

  it('archives in batch, remains idempotent, and skips legal hold', async () => {
    const first = await ctx.service.batchArchive(
      { entityType: 'invite', entityIds: ['invite-1', 'invite-hold'] },
      actor
    );
    assert.deepEqual(first.archived, ['invite-1']);
    assert(first.skipped.some((s) => s.id === 'invite-hold' && s.reason === 'legal_hold'));
    const invite = ctx.inviteModel.records.find((i) => i._id === 'invite-1');
    assert(invite.archivedAt instanceof Date);

    const second = await ctx.service.batchArchive({ entityType: 'invite', entityIds: ['invite-1'] }, actor);
    assert.deepEqual(second.archived, ['invite-1']); // idempotent
  });

  it('denies actors without compliance roles', async () => {
    await assert.rejects(
      () => ctx.service.stripPii({ entityType: 'user', entityId: 'user-1' }, { id: 'u', orgId: 'org-1', role: Role.User }),
      (err) => err instanceof ForbiddenException
    );
  });

  it('sets legal hold and logs before/after state', async () => {
    const result = await ctx.service.setLegalHold(
      { entityType: 'project', entityId: 'project-1', legalHold: true, reason: 'request' },
      actor
    );
    assert.equal(result.legalHold, true);
    const project = ctx.projectModel.records.find((p) => p._id === 'project-1');
    assert(project.legalHold);
    const event = ctx.auditLog.find((e) => e.eventType === 'compliance.legal_hold');
    assert(event);
    assert.equal(event.metadata.before, false);
    assert.equal(event.metadata.after, true);
    assert.ok(ctx.eventsCalls.some((call) => call.entityId === 'project-1' && call.legalHold === true));
  });
});
