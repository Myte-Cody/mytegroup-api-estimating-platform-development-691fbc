import { BadRequestException, ForbiddenException } from '@nestjs/common';
import { strict as assert } from 'node:assert';
import { describe, it } from 'node:test';
import { Role } from '../src/common/roles';
import { BulkService } from '../src/features/bulk/bulk.service';

type RecordShape = Record<string, any>;

const matches = (filter: RecordShape, record: RecordShape) => {
  return Object.entries(filter).every(([key, value]) => {
    const current = record[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      if ('$ne' in value) return current !== (value as any).$ne;
    }
    return current === value;
  });
};

const cloneDoc = (record: RecordShape) => ({ ...record });

const createMemoryModel = (initial: RecordShape[] = []) => {
  const store = initial.map((entry, idx) => ({
    id: entry.id || entry._id || `rec-${idx + 1}`,
    _id: entry._id || entry.id || `rec-${idx + 1}`,
    archivedAt: null,
    createdAt: entry.createdAt || new Date(),
    updatedAt: entry.updatedAt || new Date(),
    ...entry,
  }));

  return {
    store,
    async findOne(filter: RecordShape) {
      const rec = store.find((candidate) => matches(filter, candidate));
      return rec ? cloneDoc(rec) : null;
    },
    async create(doc: RecordShape) {
      const record = {
        id: doc._id || `rec-${store.length + 1}`,
        _id: doc._id || `rec-${store.length + 1}`,
        archivedAt: null,
        createdAt: new Date(),
        updatedAt: new Date(),
        ...doc,
      };
      store.push(record);
      return cloneDoc(record);
    },
    async updateOne(filter: RecordShape, update: RecordShape) {
      const index = store.findIndex((candidate) => matches(filter, candidate));
      if (index === -1) return { modifiedCount: 0 };
      store[index] = { ...store[index], ...update, updatedAt: new Date() };
      return { modifiedCount: 1 };
    },
    find(filter: RecordShape) {
      return {
        lean: async () => store.filter((candidate) => matches(filter, candidate)).map((rec) => cloneDoc(rec)),
      };
    },
  };
};

const createService = (seed?: { users?: RecordShape[]; contacts?: RecordShape[] }) => {
  const usersModel = createMemoryModel(seed?.users);
  const contactsModel = createMemoryModel(seed?.contacts);
  const projectsModel = createMemoryModel();
  const officesModel = createMemoryModel();
  const auditEvents: any[] = [];
  const audit = {
    log: async (event: any) => auditEvents.push(event),
    logMutation: async (event: any) => auditEvents.push({ ...event, eventType: `${event.entity}.${event.action}` }),
  };
  const tenants = {
    async getModelForOrg(_orgId: string, _name: string, _schema: any, defaultModel: any) {
      return defaultModel;
    },
  };
  const service = new BulkService(
    audit as any,
    tenants as any,
    usersModel as any,
    contactsModel as any,
    projectsModel as any,
    officesModel as any
  );
  return { service, auditEvents, models: { usersModel, contactsModel, projectsModel, officesModel } };
};

describe('BulkService import/export', () => {
  it('imports contacts and respects dry-run with per-row counts', async () => {
    const { service, models, auditEvents } = createService();
    const actor = { orgId: 'org-1', role: Role.Admin };

    const dryRunResult = await service.import('contacts', {
      actor,
      dryRun: true,
      records: [
        { name: 'Alice', email: 'alice@example.com', phone: '123-456-7890', tags: 'vip,priority' },
        { name: 'Bob', email: 'bob@example.com' },
      ],
    });

    assert.equal(dryRunResult.created, 2);
    assert.equal(models.contactsModel.store.length, 0, 'dry-run should not persist');

    const liveResult = await service.import('contacts', {
      actor,
      dryRun: false,
      records: [
        { name: 'Alice', email: 'alice@example.com', phone: '123-456-7890', tags: ['vip', 'priority'] },
        { name: 'Bob', email: 'bob@example.com' },
      ],
    });

    assert.equal(liveResult.created, 2);
    assert.equal(liveResult.updated, 0);
    assert.equal(liveResult.errors.length, 0);
    assert.equal(models.contactsModel.store.length, 2);
    assert.equal(auditEvents.find((evt) => evt.eventType === 'bulk.import')?.metadata?.created, 2);
  });

  it('updates existing contacts, increments updated count, and logs audit', async () => {
    const { service, models, auditEvents } = createService({
      contacts: [{ orgId: 'org-1', name: 'Alice', email: 'alice@example.com', tags: ['vip'] }],
    });
    const actor = { orgId: 'org-1', role: Role.Admin };

    const result = await service.import('contacts', {
      actor,
      records: [{ name: 'Alice Updated', email: 'alice@example.com', tags: 'vip,priority' }],
    });

    assert.equal(result.created, 0);
    assert.equal(result.updated, 1);
    const stored = models.contactsModel.store.find((rec) => rec.email === 'alice@example.com');
    assert.equal(stored?.name, 'Alice Updated');
    assert.deepEqual(stored?.tags, ['vip', 'priority']);
    const audit = auditEvents.find((evt) => evt.eventType === 'bulk.import');
    assert.equal(audit?.metadata?.updated, 1);
  });

  it('rejects unsupported columns and enforces row limit', async () => {
    const { service } = createService();
    const actor = { orgId: 'org-1', role: Role.Admin };

    const result = await service.import('contacts', {
      actor,
      records: [{ name: 'Alice', email: 'alice@example.com', unknown: 'nope' } as any],
    });
    assert.equal(result.errors.length, 1);
    assert.match(result.errors[0]?.message || '', /Unexpected columns/);

    const oversized = Array.from({ length: 1001 }, (_v, idx) => ({
      name: `Contact ${idx}`,
      email: `contact${idx}@example.com`,
    }));
    await assert.rejects(
      () => service.import('contacts', { actor, records: oversized }),
      (err: any) => err instanceof BadRequestException && /Row limit exceeded/.test(err.message)
    );
  });

  it('blocks updates on legal hold and enforces password rules for new users', async () => {
    const { service, models } = createService({
      contacts: [{ orgId: 'org-1', email: 'lock@example.com', name: 'Locked', legalHold: true }],
    });
    const actor = { orgId: 'org-1', role: Role.Admin };

    const contactResult = await service.import('contacts', {
      actor,
      records: [{ email: 'lock@example.com', name: 'Updated Name' }],
    });
    assert.equal(contactResult.errors.length, 1);
    assert.match(contactResult.errors[0]?.message || '', /legal hold/i);

    const userResult = await service.import('users', {
      actor,
      records: [{ username: 'new-user', email: 'user@example.com', password: 'weak' }],
    });
    assert.equal(userResult.errors.length, 1);
    assert.match(userResult.errors[0]?.message || '', /Password does not meet policy/);
    assert.equal(models.usersModel.store.length, 0, 'invalid password should not persist user');
  });

  it('exports JSON stripping PII when piiStripped is true', async () => {
    const { service } = createService({
      users: [
        {
          email: 'secret@example.com',
          username: 'secret',
          organizationId: 'org-1',
          piiStripped: true,
          passwordHash: 'hashed',
        },
      ],
    });
    const actor = { orgId: 'org-1', role: Role.Admin };

    const result = await service.export('users', { actor });
    assert.equal(result.format, 'json');
    assert.equal(result.count, 1);
    assert.equal(result.data[0]?.email, null, 'PII should be stripped on export');
    assert.equal(result.data[0]?.username, 'secret');
    assert.equal(result.data[0]?.redacted, true);
  });

  it('redacts exports when legalHold is true', async () => {
    const { service } = createService({
      contacts: [
        {
          orgId: 'org-1',
          name: 'Locked',
          email: 'locked@example.com',
          phone: '123-123-1234',
          legalHold: true,
          tags: ['vip'],
        },
      ],
    });
    const actor = { orgId: 'org-1', role: Role.Admin };
    const result = await service.export('contacts', { actor, includeArchived: true });
    assert.equal(result.count, 1);
    const row = result.data[0];
    assert.equal(row.email, null);
    assert.equal(row.name, null);
    assert.equal(row.phone, null);
    assert.equal(row.legalHold, true);
    assert.equal(row.redacted, true);
  });

  it('updates existing contacts, increments updated count, and logs audit', async () => {
    const { service, models, auditEvents } = createService({
      contacts: [{ orgId: 'org-1', email: 'update@example.com', name: 'Old Name' }],
    });
    const actor = { orgId: 'org-1', role: Role.Admin, id: 'actor-9' };

    const result = await service.import('contacts', {
      actor,
      records: [{ email: 'update@example.com', name: 'New Name', phone: '+19998887777' }],
    });

    assert.equal(result.created, 0);
    assert.equal(result.updated, 1);
    const contact = models.contactsModel.store.find((c) => c.email === 'update@example.com');
    assert.equal(contact?.name, 'New Name');
    assert.equal(contact?.phone, '+19998887777');
    const audit = auditEvents.find((evt) => evt.eventType === 'bulk.import');
    assert(audit);
    assert.equal(audit.metadata.updated, 1);
    assert.equal(audit.metadata.created, 0);
  });

  it('rejects compliance field changes when actor lacks privilege', async () => {
    const { service } = createService();
    const actor = { orgId: 'org-1', role: Role.User };

    await assert.rejects(
      () =>
        service.import('contacts', {
          actor,
          records: [{ name: 'A', email: 'a@example.com', legalHold: true }],
        }),
      (err: any) => err instanceof ForbiddenException
    );
  });

  it('supports includeArchived flag on export', async () => {
    const { service } = createService({
      contacts: [
        { orgId: 'org-1', name: 'Active', email: 'a@example.com', archivedAt: null },
        { orgId: 'org-1', name: 'Archived', email: 'b@example.com', archivedAt: new Date() },
      ],
    });
    const actor = { orgId: 'org-1', role: Role.Admin };

    const withoutArchived = await service.export('contacts', { actor, includeArchived: false });
    assert.equal(withoutArchived.count, 1);

    const withArchived = await service.export('contacts', { actor, includeArchived: true });
    assert.equal(withArchived.count, 2);
  });

  it('denies non-privileged roles even when orgId is present', async () => {
    const { service } = createService();
    await assert.rejects(
      () =>
        service.import('contacts', {
          actor: { orgId: 'org-1', role: Role.User },
          records: [{ name: 'Scoped', email: 'scoped@example.com' }],
        }),
      (err: any) => err instanceof ForbiddenException
    );
    await assert.rejects(
      () => service.export('contacts', { actor: { orgId: 'org-1', role: Role.User } }),
      (err: any) => err instanceof ForbiddenException
    );
  });

  it('parses CSV arrays and rejects malformed JSON payloads', async () => {
    const { service } = createService();
    const actor = { orgId: 'org-1', role: Role.Admin };

    const csv = Buffer.from('name,email,tags\nAlice,alice@example.com,"vip;priority"\n');
    const file: any = { buffer: csv, mimetype: 'text/csv', originalname: 'contacts.csv' };
    const csvResult = await service.import('contacts', { actor, file });
    assert.equal(csvResult.created, 1);

    const badJson: any = { buffer: Buffer.from('{not json'), mimetype: 'application/json', originalname: 'contacts.json' };
    await assert.rejects(
      () => service.import('contacts', { actor, file: badJson }),
      (err: any) => err instanceof BadRequestException
    );
  });

  it('requires org scope for non-superadmin actors', async () => {
    const { service } = createService();
    await assert.rejects(
      () =>
        service.import('contacts', {
          actor: { role: Role.Admin },
          records: [{ name: 'Scoped', email: 'scoped@example.com' }],
        }),
      (err: any) => err instanceof ForbiddenException
    );
  });
});
