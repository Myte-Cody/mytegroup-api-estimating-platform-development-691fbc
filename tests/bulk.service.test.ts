import { ForbiddenException, GoneException } from '@nestjs/common';
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
      if ('$gte' in value) return current >= (value as any).$gte;
      if ('$gt' in value) return current > (value as any).$gt;
      if ('$lte' in value) return current <= (value as any).$lte;
      if ('$lt' in value) return current < (value as any).$lt;
    }
    return current === value;
  });
};

const cloneDoc = (record: RecordShape) => ({ ...record });

const createMemoryModel = (initial: RecordShape[] = []) => {
  const store = initial.map((entry, idx) => ({
    id: entry.id || entry._id || `rec-${idx + 1}`,
    _id: entry._id || entry.id || `rec-${idx + 1}`,
    archivedAt: entry.archivedAt ?? null,
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

const createService = (seed?: { users?: RecordShape[]; projects?: RecordShape[]; offices?: RecordShape[] }) => {
  const usersModel = createMemoryModel(seed?.users);
  const contactsModel = createMemoryModel();
  const projectsModel = createMemoryModel(seed?.projects);
  const officesModel = createMemoryModel(seed?.offices);
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
  it('rejects bulk contacts import as deprecated', async () => {
    const { service } = createService();
    const actor = { orgId: 'org-1', role: Role.Admin };
    await assert.rejects(
      () => service.import('contacts', { actor, records: [{ name: 'Alice', email: 'alice@example.com' }] }),
      (err: any) => err instanceof GoneException
    );
  });

  it('rejects bulk contacts export as deprecated', async () => {
    const { service } = createService();
    const actor = { orgId: 'org-1', role: Role.Admin };
    await assert.rejects(
      () => service.export('contacts', { actor, includeArchived: true }),
      (err: any) => err instanceof GoneException
    );
  });

  it('imports and exports offices with dry-run support', async () => {
    const { service, models } = createService();
    const actor = { orgId: 'org-1', role: Role.Admin };

    const dryRun = await service.import('offices', {
      actor,
      dryRun: true,
      records: [{ name: 'HQ', address: '123 Main' }],
    });
    assert.equal(dryRun.created, 1);
    assert.equal(models.officesModel.store.length, 0);

    const live = await service.import('offices', {
      actor,
      records: [{ name: 'HQ', address: '123 Main' }],
    });
    assert.equal(live.created, 1);
    assert.equal(models.officesModel.store.length, 1);

    const exported: any = await service.export('offices', { actor, includeArchived: false });
    assert.equal(exported.format, 'json');
    assert.equal(exported.count, 1);
    assert.equal(exported.data[0].name, 'HQ');
  });

  it('denies non-privileged roles for office export', async () => {
    const { service } = createService();
    await assert.rejects(
      () => service.export('offices', { actor: { orgId: 'org-1', role: Role.User } }),
      (err: any) => err instanceof ForbiddenException
    );
  });
});

