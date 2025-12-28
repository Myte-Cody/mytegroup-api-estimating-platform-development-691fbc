const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { SeatsService } = require('../src/features/seats/seats.service.ts');

const matches = (filter, record) => {
  return Object.entries(filter).every(([key, value]) => {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return false;
    }
    return record[key] === value;
  });
};

const createSeatDoc = (record, records) => {
  const doc = { ...record };
  doc.save = async () => {
    const idx = records.findIndex((r) => r._id === record._id);
    if (idx >= 0) {
      records[idx] = { ...records[idx], ...doc };
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

const createSeatModel = (initial = []) => {
  const records = initial.map((seat, idx) => ({
    _id: seat._id || `seat-${idx + 1}`,
    orgId: seat.orgId || 'org-1',
    seatNumber: seat.seatNumber || idx + 1,
    status: seat.status || 'vacant',
    role: seat.role ?? null,
    userId: seat.userId ?? null,
    projectId: seat.projectId ?? null,
    activatedAt: seat.activatedAt ?? null,
    history: Array.isArray(seat.history) ? seat.history : [],
  }));

  return {
    records,
    findOneAndUpdate: async (filter, update) => {
      const record = records.find((r) => matches(filter, r));
      if (!record) return null;
      if (update.$set) Object.assign(record, update.$set);
      if (update.$unset) {
        Object.keys(update.$unset).forEach((key) => {
          record[key] = null;
        });
      }
      if (update.$push && update.$push.history) {
        record.history = Array.isArray(record.history) ? record.history : [];
        record.history.push(update.$push.history);
      }
      return createSeatDoc(record, records);
    },
    findOne: (filter) => ({
      session: () => ({
        exec: async () => {
          const record = records.find((r) => matches(filter, r));
          return record ? createSeatDoc(record, records) : null;
        },
      }),
      exec: async () => {
        const record = records.find((r) => matches(filter, r));
        return record ? createSeatDoc(record, records) : null;
      },
    }),
    countDocuments: async (filter) => records.filter((r) => matches(filter, r)).length,
    insertMany: async (docs) => {
      docs.forEach((doc) => records.push({ ...doc, _id: `seat-${records.length + 1}` }));
      return docs;
    },
  };
};

const createOrgModel = () => ({
  findOne: () => ({
    lean: async () => ({ _id: 'org-1', archivedAt: null, legalHold: false }),
  }),
});

describe('SeatsService', () => {
  it('allocates seats with role and projectId, and records history', async () => {
    const seatModel = createSeatModel([{ orgId: 'org-1', seatNumber: 1, status: 'vacant' }]);
    const auditLog = [];
    const audit = { logMutation: async (payload) => auditLog.push(payload) };
    const service = new SeatsService(seatModel, createOrgModel(), audit);

    const seat = await service.allocateSeat('org-1', 'user-1', { role: 'pm', projectId: 'proj-1' });

    assert.equal(seat.status, 'active');
    assert.equal(seat.userId, 'user-1');
    assert.equal(seat.role, 'pm');
    assert.equal(seat.projectId, 'proj-1');
    assert.equal(seat.history.length, 1);
    assert.equal(seat.history[0].role, 'pm');
    assert.equal(seat.history[0].projectId, 'proj-1');
    assert.ok(seat.history[0].assignedAt);
    assert.equal(auditLog[0]?.action, 'allocated');
  });

  it('releases seats and clears role/project while closing history entries', async () => {
    const seatModel = createSeatModel([
      {
        orgId: 'org-1',
        seatNumber: 1,
        status: 'active',
        role: 'foreman',
        userId: 'user-9',
        projectId: 'proj-9',
        history: [{ userId: 'user-9', projectId: 'proj-9', role: 'foreman', assignedAt: new Date(), removedAt: null }],
      },
    ]);
    const auditLog = [];
    const audit = { logMutation: async (payload) => auditLog.push(payload) };
    const service = new SeatsService(seatModel, createOrgModel(), audit);

    const seat = await service.releaseSeatForUser('org-1', 'user-9');

    assert.equal(seat.status, 'vacant');
    assert.equal(seat.userId, null);
    assert.equal(seat.projectId, null);
    assert.equal(seat.role, null);
    assert.equal(seat.history.length, 1);
    assert.ok(seat.history[0].removedAt);
    assert.equal(auditLog[0]?.action, 'released');
  });
});
