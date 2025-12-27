const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { ForbiddenException, ConflictException, NotFoundException } = require('@nestjs/common');
const { UsersService } = require('../src/features/users/users.service.ts');
const { Role } = require('../src/common/roles.ts');

type FakeUser = Record<string, any>;

const randomId = () => `user_${Math.random().toString(36).slice(2, 10)}`;

const createAuditStub = () => {
  const events: any[] = [];
  return {
    events,
    async log(evt: any) {
      events.push(evt);
    },
    async logMutation(evt: any) {
      events.push(evt);
    },
  };
};

const createSeatsStub = () => {
  return {
    async ensureOrgSeats() {
      return;
    },
    async allocateSeat() {
      return { status: 'active' };
    },
    async releaseSeatForUser() {
      return { status: 'vacant' };
    },
  };
};

const makeDoc = (data: FakeUser = {}) => {
  const id = data.id || data._id || randomId();
  const doc: any = {
    id,
    _id: id,
    archivedAt: null,
    isEmailVerified: true,
    isOrgOwner: false,
    piiStripped: false,
    legalHold: false,
    roles: data.roles || (data.role ? [data.role] : [Role.User]),
    role: data.role || (data.roles && data.roles[0]) || Role.User,
    ...data,
  };
  doc.toObject = () => ({ ...doc });
  doc.save = async () => doc;
  return doc;
};

const matches = (doc: FakeUser, query: FakeUser) =>
  Object.entries(query).every(([key, value]) => {
    const actual = (doc as any)[key];
    if (value && typeof value === 'object' && !(value instanceof Date) && !Array.isArray(value)) {
      if ('$gt' in value) return actual > (value as any).$gt;
      if ('$ne' in value) return actual !== (value as any).$ne;
    }
    return actual === value;
  });

const createUserModel = (seed: FakeUser[] = []) => {
  const store = seed.map(makeDoc);
  return {
    store,
    async findOne(query: FakeUser) {
      return store.find((doc) => matches(doc, query)) || null;
    },
    async find(query: FakeUser) {
      return store.filter((doc) => matches(doc, query));
    },
    async findById(id: string) {
      return store.find((doc) => doc.id === id || doc._id === id) || null;
    },
    async findByIdAndUpdate(id: string, update: FakeUser, options?: { new?: boolean; lean?: boolean }) {
      const doc = store.find((item) => item.id === id || item._id === id);
      if (!doc) return null;
      Object.assign(doc, update);
      if (options?.new) {
        return options.lean ? { ...doc } : doc;
      }
      return null;
    },
    async create(input: FakeUser) {
      const doc = makeDoc(input);
      store.push(doc);
      return doc;
    },
  };
};

describe('UsersService', () => {
  it('creates users with hashed passwords and roles array', async () => {
    const model = createUserModel();
    const audit = createAuditStub();
    const svc = new UsersService(model as any, audit as any, createSeatsStub() as any);
    const user = await svc.create({
      username: 'Jane',
      email: 'jane@example.com',
      password: 'Str0ng!Passw0rd',
      orgId: 'org1',
      role: Role.Admin,
    });
    assert.equal(user.role, Role.Admin);
    assert.deepEqual(user.roles, [Role.Admin]);
    const stored = (model as any).store[0];
    assert.ok(stored.passwordHash, 'password hash should be stored');
    assert.ok(!stored.passwordHash.includes('Str0ng!Passw0rd'));
    assert.equal(audit.events[0].eventType, 'user.created');
  });

  it('rejects creating elevated roles when actor lacks privilege', async () => {
    const model = createUserModel();
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);
    await assert.rejects(
      () =>
        svc.create(
          { username: 'Root', email: 'root@example.com', password: 'Str0ng!Passw0rd', orgId: 'orgX', role: Role.SuperAdmin },
          { role: Role.Admin, roles: [Role.Admin], orgId: 'orgX' } as any
        ),
      ForbiddenException
    );
  });

  it('scopedFindAll omits sensitive fields and ignores archived users', async () => {
    const model = createUserModel([
      {
        id: 'u1',
        username: 'Alice',
        email: 'alice@example.com',
        passwordHash: 'hash-one',
        verificationTokenHash: 'vh',
        resetTokenHash: 'rh',
        orgId: 'orgA',
      },
      {
        id: 'u2',
        username: 'Bob',
        email: 'bob@example.com',
        passwordHash: 'hash-two',
        orgId: 'orgA',
        archivedAt: new Date(),
      },
      { id: 'u3', username: 'Carl', email: 'carl@example.com', passwordHash: 'hash-three', orgId: 'orgB' },
    ]);
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);
    const results = await svc.scopedFindAll('orgA');
    assert.equal(results.length, 1);
    const user = results[0];
    assert.equal(user.orgId, 'orgA');
    assert.ok(!('passwordHash' in user));
    assert.ok(!('verificationTokenHash' in user));
    assert.ok(!('resetTokenHash' in user));
  });

  it('lists users scoped by org with includeArchived option', async () => {
    const model = createUserModel([
      { id: 'u1', username: 'Active', email: 'active@example.com', orgId: 'orgA' },
      { id: 'u2', username: 'Archived', email: 'arch@example.com', orgId: 'orgA', archivedAt: new Date() },
      { id: 'u3', username: 'Other', email: 'other@example.com', orgId: 'orgB' },
    ]);
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);

    const activeOnly = await svc.list({ orgId: 'orgA', role: Role.Admin }, { includeArchived: false });
    assert.equal(activeOnly.length, 1);
    assert.equal(activeOnly[0].id, 'u1');

    const withArchived = await svc.list({ orgId: 'orgA', role: Role.Admin }, { includeArchived: true });
    assert.equal(withArchived.length, 2);

    await assert.rejects(() => svc.list({ role: Role.Admin } as any), ForbiddenException);
  });

  it('enforces org scoping for archive/unarchive and stays idempotent', async () => {
    const target = makeDoc({
      id: 'u-archive',
      username: 'Target',
      email: 'target@example.com',
      orgId: 'orgZ',
    });
    const model = createUserModel([target]);
    const audit = createAuditStub();
    const svc = new UsersService(model as any, audit as any, createSeatsStub() as any);

    await assert.rejects(
      () => svc.archiveUser('u-archive', { role: Role.Admin, orgId: 'other-org' }),
      (err: any) => err instanceof ForbiddenException
    );

    const archived = await svc.archiveUser('u-archive', { role: Role.Admin, orgId: 'orgZ' });
    assert.ok(archived.archivedAt);
    const archivedAgain = await svc.archiveUser('u-archive', { role: Role.Admin, orgId: 'orgZ' });
    assert.equal(archivedAgain.archivedAt?.valueOf(), archived.archivedAt?.valueOf());
    const archiveEvents = audit.events.filter((e) => e.eventType === 'user.archived');
    assert.equal(archiveEvents.length, 1);

    const unarchived = await svc.unarchiveUser('u-archive', { role: Role.Admin, orgId: 'orgZ' });
    assert.equal(unarchived.archivedAt, null);
    const unarchivedAgain = await svc.unarchiveUser('u-archive', { role: Role.Admin, orgId: 'orgZ' });
    assert.equal(unarchivedAgain.archivedAt, null);
    const unarchiveEvents = audit.events.filter((e) => e.eventType === 'user.unarchived');
    assert.equal(unarchiveEvents.length, 1);
  });

  it('blocks archive/unarchive when legal hold is set', async () => {
    const target = makeDoc({
      id: 'u-held',
      username: 'Held',
      email: 'held@example.com',
      orgId: 'orgZ',
      legalHold: true,
    });
    const model = createUserModel([target]);
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);

    await assert.rejects(
      () => svc.archiveUser('u-held', { role: Role.Admin, orgId: 'orgZ' }),
      (err: any) => err instanceof ForbiddenException
    );
    await assert.rejects(
      () => svc.unarchiveUser('u-held', { role: Role.Admin, orgId: 'orgZ' }),
      (err: any) => err instanceof ForbiddenException
    );
  });

  it('updates roles with RBAC validation and audit logging', async () => {
    const model = createUserModel([
      { id: 'u-role', username: 'RoleUser', email: 'role@example.com', orgId: 'orgR', role: Role.User },
    ]);
    const audit = createAuditStub();
    const svc = new UsersService(model as any, audit as any, createSeatsStub() as any);

    await assert.rejects(
      () => svc.updateRoles('u-role', [Role.SuperAdmin], { role: Role.Admin, orgId: 'orgR' }),
      (err: any) => err instanceof ForbiddenException
    );

    await assert.rejects(
      () => svc.updateRoles('u-role', [Role.Admin], { role: Role.PM, orgId: 'orgR' }),
      (err: any) => err instanceof ForbiddenException
    );

    const updated = await svc.updateRoles(
      'u-role',
      [Role.Admin, Role.Estimator],
      { role: Role.Admin, orgId: 'orgR' }
    );
    assert.equal(updated.role, Role.Admin);
    assert.deepEqual(updated.roles.sort(), [Role.Admin, Role.Estimator].sort());
    assert.equal(audit.events.at(-1)?.eventType, 'user.roles_updated');
  });

  it('fetches users by id with archive flag respected', async () => {
    const archivedAt = new Date();
    const model = createUserModel([
      { id: 'u-active', username: 'Active', email: 'active@example.com', orgId: 'orgX' },
      { id: 'u-arch', username: 'Archived', email: 'arch@example.com', orgId: 'orgX', archivedAt },
    ]);
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);
    const actor = { orgId: 'orgX', role: Role.Admin };

    const active = await svc.getById('u-active', actor as any);
    assert.equal(active.id, 'u-active');
    await assert.rejects(() => svc.getById('u-arch', actor as any), NotFoundException);
    const archived = await svc.getById('u-arch', actor as any, true);
    assert.equal(archived.id, 'u-arch');
  });

  it('updates profile fields with compliance guard and audit diff', async () => {
    const model = createUserModel([
      { id: 'u-update', username: 'User', email: 'user@example.com', orgId: 'orgY', piiStripped: false, legalHold: false },
    ]);
    const audit = createAuditStub();
    const svc = new UsersService(model as any, audit as any, createSeatsStub() as any);
    const actor = { orgId: 'orgY', role: Role.Admin };

    const updated = await svc.update(
      'u-update',
      { username: 'Renamed', piiStripped: true, isEmailVerified: true },
      actor as any
    );

    assert.equal(updated.username, 'Renamed');
    assert.equal(updated.piiStripped, true);
    assert.equal(updated.isEmailVerified, true);
    const auditEvent = audit.events.find((evt) => evt.eventType === 'user.updated');
    assert.ok(auditEvent);
    assert.ok(auditEvent.metadata.changes.username);
  });

  it('prevents compliance updates without privilege and prevents duplicate emails', async () => {
    const model = createUserModel([
      { id: 'u1', username: 'User1', email: 'one@example.com', orgId: 'orgZ' },
      { id: 'u2', username: 'User2', email: 'two@example.com', orgId: 'orgZ' },
    ]);
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);
    await assert.rejects(
      () => svc.update('u1', { piiStripped: true }, { orgId: 'orgZ', role: Role.User } as any),
      ForbiddenException
    );
    await assert.rejects(
      () => svc.update('u1', { email: 'two@example.com' }, { orgId: 'orgZ', role: Role.Admin } as any),
      ConflictException
    );
  });

  it('applies expiry and legal hold checks for verification/reset tokens', async () => {
    const model = createUserModel([
      {
        id: 'blocked',
        username: 'Blocked',
        email: 'blocked@example.com',
        orgId: 'orgT',
        verificationTokenHash: 'same-hash',
        verificationTokenExpires: new Date(Date.now() - 1000),
        resetTokenHash: 'same-reset',
        resetTokenExpires: new Date(Date.now() - 1000),
        legalHold: true,
      },
      {
        id: 'ok',
        username: 'Active',
        email: 'ok@example.com',
        orgId: 'orgT',
        verificationTokenHash: 'same-hash',
        verificationTokenExpires: new Date(Date.now() + 1000),
        resetTokenHash: 'same-reset',
        resetTokenExpires: new Date(Date.now() + 1000),
        legalHold: false,
      },
    ]);
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);
    const verify = await svc.findByVerificationToken('same-hash');
    assert.equal(verify?.id, 'ok');
    const reset = await svc.findByResetToken('same-reset');
    assert.equal(reset?.id, 'ok');
  });

  it('tracks last login timestamps', async () => {
    const model = createUserModel([{ id: 'u-last', username: 'Last', email: 'last@example.com', orgId: 'orgL' }]);
    const svc = new UsersService(model as any, createAuditStub() as any, createSeatsStub() as any);
    const updated = await svc.markLastLogin('u-last');
    assert.ok(updated?.lastLogin instanceof Date);
  });
});
