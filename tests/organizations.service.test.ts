const assert = require('node:assert/strict');
const { beforeEach, describe, it } = require('node:test');
const { BadRequestException, ForbiddenException } = require('@nestjs/common');
const { OrganizationsService } = require('../src/features/organizations/organizations.service.ts');

type FakeOrgRecord = {
  _id: string;
  id: string;
  name: string;
  metadata: Record<string, unknown>;
  useDedicatedDb: boolean;
  datastoreType: 'shared' | 'dedicated';
  databaseUri: string | null;
  databaseName: string | null;
  dataResidency: 'shared' | 'dedicated';
  archivedAt: Date | null;
  legalHold: boolean;
  piiStripped: boolean;
  datastoreHistory: any[];
  createdAt: Date;
  updatedAt: Date;
};

const matches = (filter: Record<string, any>, record: FakeOrgRecord) => {
  return Object.entries(filter).every(([key, value]) => {
    const current = (record as any)[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      if ('$ne' in value) return current !== value.$ne;
    }
    return current === value;
  });
};

const createDoc = (record: FakeOrgRecord, records: FakeOrgRecord[]) => {
  const doc: any = { ...record };
  doc.save = async () => {
    const idx = records.findIndex((r) => r._id === record._id);
    if (idx >= 0) {
      records[idx] = { ...records[idx], ...doc, updatedAt: new Date() };
      Object.assign(doc, records[idx]);
    }
    return doc;
  };
  doc.toObject = () => {
    const { save, toObject, ...rest } = doc;
    return { ...rest };
  };
  return doc;
};

const createOrgModel = (initial: Partial<FakeOrgRecord>[] = []) => {
  const records: FakeOrgRecord[] = initial.map((entry, idx) => {
    const id = entry._id || entry.id || `org-${idx + 1}`;
    return {
      _id: id,
      id,
      name: entry.name || `Org ${idx + 1}`,
      metadata: entry.metadata || {},
      useDedicatedDb: entry.useDedicatedDb ?? false,
      datastoreType: entry.datastoreType || 'shared',
      databaseUri: entry.databaseUri ?? null,
      databaseName: entry.databaseName ?? null,
      dataResidency: entry.dataResidency || 'shared',
      archivedAt: entry.archivedAt ?? null,
      legalHold: entry.legalHold ?? false,
      piiStripped: entry.piiStripped ?? false,
      datastoreHistory: entry.datastoreHistory || [],
      createdAt: entry.createdAt || new Date(),
      updatedAt: entry.updatedAt || new Date(),
    };
  });

  return {
    records,
    async findOne(filter: Record<string, any>) {
      const rec = records.find((candidate) => matches(filter, candidate));
      return rec ? createDoc(rec, records) : null;
    },
    async findById(id: string) {
      const rec = records.find((candidate) => candidate._id === id);
      return rec ? createDoc(rec, records) : null;
    },
    async create(payload: Partial<FakeOrgRecord>) {
      const id = payload._id || payload.id || `org-${records.length + 1}`;
      const record: FakeOrgRecord = {
        _id: id,
        id,
        name: payload.name || id,
        metadata: payload.metadata || {},
        useDedicatedDb: payload.useDedicatedDb ?? false,
        datastoreType: (payload.datastoreType as any) || (payload.useDedicatedDb ? 'dedicated' : 'shared'),
        databaseUri: payload.databaseUri ?? null,
        databaseName: payload.databaseName ?? null,
        dataResidency: (payload.dataResidency as any) || 'shared',
        archivedAt: payload.archivedAt ?? null,
        legalHold: payload.legalHold ?? false,
        piiStripped: payload.piiStripped ?? false,
        datastoreHistory: payload.datastoreHistory || [],
        createdAt: payload.createdAt || new Date(),
        updatedAt: payload.updatedAt || new Date(),
      };
      records.push(record);
      return createDoc(record, records);
    },
    find(filter: Record<string, any>) {
      const filtered = records.filter((rec) => matches(filter, rec));
      return {
        lean: async () => filtered.map((rec) => ({ ...rec })),
      };
    },
  };
};

const createService = (initial: Partial<FakeOrgRecord>[] = [], opts?: { failConnection?: boolean }) => {
  const model = createOrgModel(initial);
  const auditLog: any[] = [];
  const audit = { log: async (event: any) => auditLog.push(event) };
  const resets: string[] = [];
  const testCalls: any[] = [];
  const tenants = {
    resetConnectionForOrg: async (id: string) => resets.push(id),
    testConnection: async (uri: string, dbName?: string) => {
      testCalls.push({ uri, dbName });
      if (opts?.failConnection) throw new Error('connection failed');
    },
  };

  const svc = new OrganizationsService(model as any, audit as any, tenants as any);
  return { svc, auditLog, resets, testCalls, model };
};

describe('OrganizationsService', () => {
  let initial: Partial<FakeOrgRecord>[];

  beforeEach(() => {
    initial = [
      {
        _id: 'org-1',
        name: 'Alpha',
        metadata: { tier: 'basic' },
        useDedicatedDb: false,
        datastoreType: 'shared',
        databaseUri: null,
        databaseName: null,
        dataResidency: 'shared',
        archivedAt: null,
        legalHold: false,
        piiStripped: false,
        datastoreHistory: [],
      },
    ];
  });

  it('switches datastore to dedicated, validates connection, and resets tenant cache', async () => {
    const { svc, auditLog, resets, testCalls, model } = createService(initial);

    const updated = await svc.updateDatastore(
      'org-1',
      { type: 'dedicated', datastoreUri: 'mongodb://example', databaseName: 'tenant-db' },
      { id: 'actor-1' }
    );

    assert.equal(updated.useDedicatedDb, true);
    assert.equal(updated.datastoreType, 'dedicated');
    assert.equal(updated.databaseUri, 'mongodb://example');
    assert.equal(updated.databaseName, 'tenant-db');
    assert.equal(model.records[0].datastoreHistory.length, 1);
    assert.equal(model.records[0].datastoreHistory[0].actorId, 'actor-1');
    assert.equal(testCalls.length, 1);
    assert.deepEqual(resets, ['org-1']);
    assert.equal(auditLog[auditLog.length - 1]?.eventType, 'organization.datastore_switched');
    assert.equal(auditLog[auditLog.length - 1]?.metadata?.after?.databaseName, 'tenant-db');
    assert.ok(!('databaseUri' in (auditLog[auditLog.length - 1]?.metadata?.after || {})));
  });

  it('blocks datastore switch when org is on legal hold', async () => {
    initial[0].legalHold = true;
    const { svc } = createService(initial);
    await assert.rejects(
      () => svc.updateDatastore('org-1', { type: 'dedicated', datastoreUri: 'mongodb://example' }),
      (err: any) => err instanceof ForbiddenException
    );
  });

  it('throws when dedicated datastore lacks uri', async () => {
    const { svc } = createService(initial);
    await assert.rejects(
      () => svc.updateDatastore('org-1', { type: 'dedicated' }),
      (err: any) => err instanceof BadRequestException && /connection URI/.test(err.message)
    );
  });

  it('archives and unarchives idempotently while emitting audit events on change', async () => {
    const { svc, auditLog, model } = createService(initial);

    const archived = await svc.archive('org-1', { id: 'actor-2' });
    assert(archived.archivedAt instanceof Date);
    const firstAudit = auditLog.find((evt) => evt.eventType === 'organization.archived');
    assert(firstAudit);

    // idempotent archive should not add another event
    await svc.archive('org-1', { id: 'actor-2' });
    const archivedEvents = auditLog.filter((evt) => evt.eventType === 'organization.archived');
    assert.equal(archivedEvents.length, 1);

    const unarchived = await svc.unarchive('org-1', { id: 'actor-2' });
    assert.equal(unarchived.archivedAt, null);
    const unarchivedEvents = auditLog.filter((evt) => evt.eventType === 'organization.unarchived');
    assert.equal(unarchivedEvents.length, 1);
    assert.equal(model.records[0].archivedAt, null);
  });

  it('prevents archive when org is on legal hold', async () => {
    initial[0].legalHold = true;
    const { svc } = createService(initial);
    await assert.rejects(
      () => svc.archive('org-1', { id: 'actor-3' }),
      (err: any) => err instanceof ForbiddenException
    );
  });

  it('toggles legal hold and pii flags with audit logging', async () => {
    const { svc, auditLog, model } = createService(initial);

    const held = await svc.setLegalHold('org-1', { legalHold: true }, { id: 'actor-4' });
    assert.equal(held.legalHold, true);
    assert.equal(model.records[0].legalHold, true);
    assert.equal(auditLog[auditLog.length - 1]?.eventType, 'organization.legal_hold_toggled');

    const pii = await svc.setPiiStripped('org-1', { piiStripped: true }, { id: 'actor-4' });
    assert.equal(pii.piiStripped, true);
    assert.equal(model.records[0].piiStripped, true);
    assert.equal(auditLog[auditLog.length - 1]?.eventType, 'organization.pii_stripped_toggled');
  });
});
