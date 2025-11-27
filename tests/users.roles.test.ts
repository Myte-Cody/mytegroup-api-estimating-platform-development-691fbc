const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { ForbiddenException } = require('@nestjs/common');
const { Role } = require('../src/common/roles.ts');
const { UsersService } = require('../src/features/users/users.service.ts');

const matches = (filter: Record<string, any>, record: Record<string, any>) => {
  return Object.entries(filter).every(([key, value]) => {
    const current = record[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      if ('$ne' in value) return current !== (value as any).$ne;
      if ('$gt' in value) return current > (value as any).$gt;
      if ('$gte' in value) return current >= (value as any).$gte;
    }
    return current === value;
  });
};

const createDoc = (record: any, records: any[]) => {
  const doc: any = { ...record };
  doc.save = async () => {
    const idx = records.findIndex((r) => r._id === doc._id);
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

const createUserModel = (initial: any[] = []) => {
  const records = initial.map((entry, idx) => {
    const id = entry._id || entry.id || `user-${idx + 1}`;
    return {
      _id: id,
      id,
      username: entry.username || `user-${idx + 1}`,
      email: entry.email || `user${idx + 1}@example.com`,
      passwordHash: entry.passwordHash || 'hashed',
      role: entry.role || Role.User,
      roles: entry.roles || [entry.role || Role.User],
      organizationId: entry.organizationId || 'org-1',
      archivedAt: entry.archivedAt ?? null,
      legalHold: entry.legalHold ?? false,
      isOrgOwner: entry.isOrgOwner ?? (entry.roles || []).includes(Role.OrgOwner),
      lastLogin: entry.lastLogin ?? null,
    };
  });
  const toDoc = (rec: any) => createDoc({ ...rec }, records);
  return {
    records,
    findOne: async (filter: Record<string, any>) => {
      const rec = records.find((r) => matches(filter, r));
      return rec ? toDoc(rec) : null;
    },
    findById: async (id: string) => {
      const rec = records.find((r) => r._id === id || r.id === id);
      return rec ? toDoc(rec) : null;
    },
    find: async (filter: Record<string, any>) => {
      return records.filter((r) => matches(filter, r)).map((r) => toDoc(r));
    },
    findByIdAndUpdate: async (id: string, update: Record<string, any>) => {
      const rec = records.find((r) => r._id === id || r.id === id);
      if (!rec) return null;
      Object.assign(rec, update);
      return toDoc(rec);
    },
    create: async (dto: any) => {
      const id = dto._id || dto.id || `user-${records.length + 1}`;
      const rec = {
        _id: id,
        id,
        ...dto,
      };
      records.push(rec);
      return toDoc(rec);
    },
  };
};

const createService = (initial: any[] = []) => {
  const auditLog: any[] = [];
  const model = createUserModel(initial) as any;
  const service = new UsersService(model, {
    log: async (evt: any) => auditLog.push(evt),
  } as any);
  return { service, model, auditLog };
};

describe('UsersService role management', () => {
  it('sets primary role based on priority and emits audit metadata', async () => {
    const { service, auditLog, model } = createService([
      { _id: 'u1', organizationId: 'org-1', roles: [Role.User], role: Role.User },
    ]);
    const result = await service.updateRoles(
      'u1',
      [Role.Estimator, Role.Admin],
      { role: Role.SuperAdmin, roles: [Role.SuperAdmin], orgId: 'org-1' } as any
    );

    assert.equal(result.role, Role.Admin);
    assert.deepEqual(new Set(result.roles), new Set([Role.Estimator, Role.Admin]));
    const latestAudit = auditLog[auditLog.length - 1];
    assert.equal(latestAudit?.eventType, 'user.roles_updated');
    assert.deepEqual(latestAudit?.metadata.roles, [Role.Estimator, Role.Admin]);
    const stored = model.records.find((r: any) => r._id === 'u1');
    assert.equal(stored?.role, Role.Admin);
    assert.equal(stored?.isOrgOwner, false);
  });

  it('prevents assigning superadmin when actor lacks privilege', async () => {
    const { service } = createService([{ _id: 'u2', organizationId: 'org-1' }]);
    await assert.rejects(
      () =>
        service.updateRoles('u2', [Role.SuperAdmin], {
          role: Role.Admin,
          roles: [Role.Admin],
          orgId: 'org-1',
        } as any),
      ForbiddenException
    );
  });

  it('blocks cross-org updates for non-privileged actors', async () => {
    const { service } = createService([{ _id: 'u3', organizationId: 'org-1' }]);
    await assert.rejects(
      () =>
        service.updateRoles('u3', [Role.Admin], {
          role: Role.Admin,
          roles: [Role.Admin],
          orgId: 'other-org',
        } as any),
      ForbiddenException
    );
  });

  it('allows platform admin to manage roles without org constraint', async () => {
    const { service } = createService([{ _id: 'u4', organizationId: 'org-1', roles: [Role.User] }]);
    const result = await service.updateRoles(
      'u4',
      [Role.Manager],
      { roles: [Role.PlatformAdmin] } as any
    );

    assert.equal(result.role, Role.Manager);
    assert.deepEqual(result.roles, [Role.Manager]);
  });

  it('prevents role updates for archived users', async () => {
    const { service } = createService([
      { _id: 'u5', organizationId: 'org-1', archivedAt: new Date() },
    ]);
    await assert.rejects(
      () =>
        service.updateRoles('u5', [Role.Manager], {
          role: Role.SuperAdmin,
          roles: [Role.SuperAdmin],
        } as any),
      ForbiddenException
    );
  });
});
