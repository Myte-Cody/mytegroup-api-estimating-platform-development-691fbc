const { BadRequestException, ForbiddenException, NotFoundException } = require('@nestjs/common');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const { beforeEach, describe, it } = require('node:test');
const { Role } = require('../src/common/roles.ts');
const { InvitesService } = require('../src/features/invites/invites.service.ts');

type RoleType = (typeof Role)[keyof typeof Role];

type InviteStatus = 'pending' | 'accepted' | 'expired';

type FakeInviteRecord = {
  _id: string;
  id: string;
  orgId: string;
  email: string;
  role: RoleType;
  personId?: string | null;
  tokenHash: string | null;
  tokenExpires: Date;
  status: InviteStatus;
  createdByUserId: string;
  invitedUserId?: string | null;
  acceptedAt?: Date | null;
  archivedAt?: Date | null;
  piiStripped?: boolean;
  legalHold?: boolean;
  createdAt: Date;
  updatedAt: Date;
};

const hashToken = (token: string) => crypto.createHash('sha256').update(token).digest('hex');

const matches = (filter: Record<string, any>, record: FakeInviteRecord) => {
  return Object.entries(filter).every(([key, value]) => {
    const current = (record as Record<string, any>)[key];
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      if ('$lte' in value) return current <= value.$lte;
      if ('$lt' in value) return current < value.$lt;
      if ('$gt' in value) return current > value.$gt;
      if ('$gte' in value) return current >= value.$gte;
    }
    return current === value;
  });
};

const createDoc = (record: FakeInviteRecord, records: FakeInviteRecord[]) => {
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

const createInviteModel = (initial: Partial<FakeInviteRecord>[] = []) => {
  const records: FakeInviteRecord[] = initial.map((entry, idx) => {
    const id = entry._id || entry.id || `invite-${idx + 1}`;
    return {
      _id: id,
      id,
      orgId: entry.orgId || 'org-default',
      email: entry.email || 'user@example.com',
      role: (entry.role as RoleType) || Role.User,
      personId: entry.personId ?? null,
      tokenHash: entry.tokenHash ?? hashToken('seed-token'),
      tokenExpires: entry.tokenExpires || new Date(Date.now() + 60 * 60 * 1000),
      status: entry.status || 'pending',
      createdByUserId: entry.createdByUserId || 'creator-1',
      invitedUserId: entry.invitedUserId ?? null,
      acceptedAt: entry.acceptedAt ?? null,
      archivedAt: entry.archivedAt ?? null,
      piiStripped: entry.piiStripped ?? false,
      legalHold: entry.legalHold ?? false,
      createdAt: entry.createdAt || new Date(),
      updatedAt: entry.updatedAt || new Date(),
    };
  });

  return {
    db: {
      startSession: async () => {
        return {
          startTransaction: () => undefined,
          commitTransaction: async () => undefined,
          abortTransaction: async () => undefined,
          endSession: () => undefined,
        };
      },
    },
    records,
    async updateMany(filter: Record<string, any>, update: Record<string, any>) {
      let modified = 0;
      records.forEach((rec, idx) => {
        if (matches(filter, rec)) {
          records[idx] = { ...rec, ...update, updatedAt: new Date() };
          modified += 1;
        }
      });
      return { modifiedCount: modified };
    },
    async findOne(filter: Record<string, any>) {
      const rec = records.find((candidate) => matches(filter, candidate));
      return rec ? createDoc(rec, records) : null;
    },
    async countDocuments(filter: Record<string, any>) {
      return records.filter((rec) => matches(filter, rec)).length;
    },
    async create(doc: Partial<FakeInviteRecord>) {
      const now = new Date();
      const id = `invite-${records.length + 1}`;
      const record: FakeInviteRecord = {
        _id: id,
        id,
        orgId: doc.orgId || 'org-default',
        email: doc.email || 'user@example.com',
        role: (doc.role as RoleType) || Role.User,
        personId: doc.personId ?? null,
        tokenHash: doc.tokenHash ?? hashToken('token'),
        tokenExpires: doc.tokenExpires || new Date(now.getTime() + 72 * 60 * 60 * 1000),
        status: (doc.status as InviteStatus) || 'pending',
        createdByUserId: doc.createdByUserId || 'creator-1',
        invitedUserId: doc.invitedUserId ?? null,
        acceptedAt: doc.acceptedAt ?? null,
        archivedAt: doc.archivedAt ?? null,
        piiStripped: doc.piiStripped ?? false,
        legalHold: doc.legalHold ?? false,
        createdAt: doc.createdAt || now,
        updatedAt: doc.updatedAt || now,
      };
      records.push(record);
      return createDoc(record, records);
    },
    find(filter: Record<string, any>) {
      return {
        lean: async () => records.filter((rec) => matches(filter, rec)).map((rec) => ({ ...rec })),
      };
    },
  };
};

const createService = (
  model: ReturnType<typeof createInviteModel>,
  tokenFactory?: (hours: number) => { token: string; hash: string; expires: Date },
  personResolver?: (input: { orgId: string; personId: string }) => Record<string, any>
) => {
  const auditLog: any[] = [];
  const emailLog: any[] = [];
  const usersCreated: any[] = [];
  const personLinks: any[] = [];

  const audit = { log: async (event: any) => auditLog.push(event) };
  const users = {
    create: async (dto: any) => {
      const user = { id: `user-${usersCreated.length + 1}`, ...dto };
      usersCreated.push(user);
      return user;
    },
    findAnyByEmail: async () => null,
  };
  const email = { sendInviteEmail: async (email: string, token: string) => emailLog.push({ email, token }) };

  const defaultPerson = (orgId: string, personId: string) => ({
    _id: personId,
    id: personId,
    orgId,
    personType: 'internal_staff',
    primaryEmail: String(personId || '').trim().toLowerCase().includes('@')
      ? String(personId || '').trim().toLowerCase()
      : 'user@example.com',
    ironworkerNumber: null,
    userId: null,
    archivedAt: null,
  });

  const persons = {
    getById: async (_actor: any, orgId: string, personId: string) => {
      if (personResolver) {
        return { ...defaultPerson(orgId, personId), ...personResolver({ orgId, personId }) };
      }
      return defaultPerson(orgId, personId);
    },
    findByPrimaryEmail: async (_actor: any, orgId: string, email: string) => ({
      _id: 'person-1',
      id: 'person-1',
      orgId,
      personType: 'internal_staff',
      primaryEmail: String(email || '').trim().toLowerCase(),
      ironworkerNumber: null,
      userId: null,
      archivedAt: null,
    }),
    linkUser: async (orgId: string, personId: string, userId: string) => {
      personLinks.push({ orgId, personId, userId });
    },
  };

  const seats = { ensureOrgSeats: async () => undefined };
  const service = new InvitesService(
    model as any,
    audit as any,
    users as any,
    email as any,
    persons as any,
    seats as any
  );
  if (tokenFactory) {
    (service as any).generateToken = tokenFactory;
  }

  return { service, auditLog, emailLog, usersCreated, personLinks };
};

describe('InvitesService', () => {
  let model: ReturnType<typeof createInviteModel>;

  beforeEach(() => {
    model = createInviteModel();
  });

  it('denies inviting elevated roles when actor lacks privilege (RBAC deny)', async () => {
    const { service } = createService(model);
    await assert.rejects(
      () =>
        service.create('org-1', 'actor-1', Role.PM, {
          personId: 'candidate@example.com',
          role: Role.Admin,
        }),
      (err: any) => err instanceof ForbiddenException && /Insufficient role/.test(err.message)
    );
  });

  it('allows superintendent invites from internal_union (ironworker) people', async () => {
    const { service } = createService(model, undefined, () => ({
      personType: 'internal_union',
    }));

    const invite = await service.create('org-1', 'actor-1', Role.Admin, {
      personId: 'person-1',
      role: Role.Superintendent,
    });

    assert.equal(invite.role, Role.Superintendent);
  });

  it('rejects staff-only roles for internal_union people', async () => {
    const { service } = createService(model, undefined, () => ({
      personType: 'internal_union',
    }));

    await assert.rejects(
      () =>
        service.create('org-1', 'actor-1', Role.Admin, {
          personId: 'person-2',
          role: Role.PM,
        }),
      (err: any) => err instanceof BadRequestException && /internal_staff/.test(err.message)
    );
  });

  it('rejects foreman invites without ironworkerNumber', async () => {
    const { service } = createService(model, undefined, () => ({
      personType: 'internal_union',
      ironworkerNumber: null,
    }));

    await assert.rejects(
      () =>
        service.create('org-1', 'actor-1', Role.Admin, {
          personId: 'person-3',
          role: Role.Foreman,
        }),
      (err: any) => err instanceof BadRequestException && /ironworkerNumber/.test(err.message)
    );
  });

  it('rejects superintendent invites from external people', async () => {
    const { service } = createService(model, undefined, () => ({
      personType: 'external_person',
    }));

    await assert.rejects(
      () =>
        service.create('org-1', 'actor-1', Role.Admin, {
          personId: 'person-4',
          role: Role.Superintendent,
        }),
      (err: any) => err instanceof BadRequestException && /Superintendent invites/.test(err.message)
    );
  });

  it('rejects duplicate pending invites for the same email', async () => {
    const expires = new Date(Date.now() + 2 * 60 * 60 * 1000);
    model = createInviteModel([
      {
        _id: 'invite-existing',
        orgId: 'org-1',
        email: 'dupe@example.com',
        role: Role.User,
        tokenHash: hashToken('existing'),
        tokenExpires: expires,
        status: 'pending',
      },
    ]);
    const { service } = createService(model);
    await assert.rejects(
      () =>
        service.create('org-1', 'actor-1', Role.Admin, {
          personId: 'dupe@example.com',
          role: Role.User,
        }),
      (err: any) => err instanceof BadRequestException && /Pending invite already exists/.test(err.message)
    );
  });

  it('creates an invite, sends email, and logs audit with requested TTL', async () => {
    const expires = new Date('2025-01-01T00:00:00Z');
    let requestedHours = 0;
    const tokenFactory = (hours: number) => {
      requestedHours = hours;
      return { token: 'fixed-token', hash: 'fixed-hash', expires };
    };
    const { service, emailLog, auditLog } = createService(model, tokenFactory);

    const invite = await service.create('org-1', 'actor-1', Role.Admin, {
      personId: 'new@example.com',
      role: Role.PM,
      expiresInHours: 12,
    });

    assert.equal(invite.email, 'new@example.com');
    assert.equal(requestedHours, 12);
    const stored = model.records.find((rec) => rec.email === 'new@example.com');
    assert(stored);
    assert.equal(stored.personId, 'new@example.com');
    assert.equal(stored?.tokenHash, 'fixed-hash');
    assert.equal(stored?.tokenExpires.toISOString(), expires.toISOString());
    assert.deepEqual(emailLog, [{ email: 'new@example.com', token: 'fixed-token' }]);
    assert.equal(auditLog[0]?.eventType, 'invite.created');
  });

  it('resend regenerates token, extends TTL, and emits audit log', async () => {
    const initialExpires = new Date('2024-12-31T00:00:00Z');
    model = createInviteModel([
      {
        _id: 'invite-1',
        orgId: 'org-1',
        email: 'resend@example.com',
        role: Role.User,
        tokenHash: 'old-hash',
        tokenExpires: initialExpires,
        status: 'pending',
      },
    ]);
    const newExpires = new Date('2025-02-01T00:00:00Z');
    const { service, emailLog, auditLog } = createService(model, () => ({
      token: 'resent-token',
      hash: 'new-hash',
      expires: newExpires,
    }));

    await service.resend('org-1', 'invite-1', 'actor-1');

    const updated = model.records.find((rec) => rec._id === 'invite-1');
    assert(updated);
    assert.equal(updated?.tokenHash, 'new-hash');
    assert.notEqual(updated?.tokenHash, 'old-hash');
    assert.equal(updated?.tokenExpires.toISOString(), newExpires.toISOString());
    assert.equal(emailLog[emailLog.length - 1]?.token, 'resent-token');
    assert.equal(auditLog[auditLog.length - 1]?.eventType, 'invite.resent');
    assert(updated.tokenExpires > initialExpires);
  });

  it('rejects expired token on accept and marks invite expired (token TTL)', async () => {
    const expiredToken = 'expired-token';
    const expiredHash = hashToken(expiredToken);
    const past = new Date(Date.now() - 60 * 60 * 1000);
    model = createInviteModel([
      {
        _id: 'invite-expired',
        orgId: 'org-1',
        email: 'expired@example.com',
        role: Role.User,
        tokenHash: expiredHash,
        tokenExpires: past,
        status: 'pending',
      },
    ]);
    const { service } = createService(model);

    await assert.rejects(
      () =>
        service.accept({
          token: expiredToken,
          username: 'u',
          password: 'P@ssw0rd!123',
        }),
      (err: any) => err instanceof BadRequestException && /Invite expired/.test(err.message)
    );

    const updated = model.records.find((rec) => rec._id === 'invite-expired');
    assert(updated);
    assert.equal(updated.status, 'expired');
  });

  it('rejects unknown token during accept', async () => {
    const validHash = hashToken('valid-token');
    model = createInviteModel([
      {
        _id: 'invite-valid',
        orgId: 'org-1',
        email: 'valid@example.com',
        role: Role.User,
        tokenHash: validHash,
        tokenExpires: new Date(Date.now() + 60 * 60 * 1000),
        status: 'pending',
      },
    ]);
    const { service } = createService(model);

    await assert.rejects(
      () =>
        service.accept({
          token: 'wrong-token',
          username: 'u',
          password: 'P@ssw0rd!123',
        }),
      (err: any) => err instanceof NotFoundException && /Invite not found or expired/.test(err.message)
    );
  });

  it('prevents single-use token reuse after acceptance', async () => {
    const acceptedToken = 'accepted';
    model = createInviteModel([
      {
        _id: 'invite-accepted',
        orgId: 'org-1',
        email: 'accepted@example.com',
        role: Role.User,
        tokenHash: hashToken(acceptedToken),
        tokenExpires: new Date(Date.now() + 60 * 60 * 1000),
        status: 'accepted',
      },
    ]);
    const { service } = createService(model);

    await assert.rejects(
      () =>
        service.accept({
          token: acceptedToken,
          username: 'u',
          password: 'P@ssw0rd!123',
        }),
      (err: any) => err instanceof BadRequestException && /already accepted/.test(err.message)
    );

    const persisted = model.records.find((rec) => rec._id === 'invite-accepted');
    assert(persisted);
    assert.equal(persisted.status, 'accepted');
  });

  it('accepts invite, creates user, links person, and emits audit', async () => {
    const token = 'accept-me';
    const tokenHash = hashToken(token);
    model = createInviteModel([
      {
        _id: 'invite-to-accept',
        orgId: 'org-1',
        email: 'join@example.com',
        role: Role.Engineer,
        tokenHash,
        tokenExpires: new Date(Date.now() + 2 * 60 * 60 * 1000),
        status: 'pending',
        personId: 'person-77',
      },
    ]);
    const { service, usersCreated, personLinks, auditLog } = createService(model);

    const result = await service.accept({
      token,
      username: 'new-user',
      password: 'P@ssw0rd!123',
    });

    assert.equal(result.user.email, 'join@example.com');
    assert.equal(usersCreated[0]?.orgId, 'org-1');
    assert.equal(usersCreated[0]?.role, Role.Engineer);
    const invite = model.records.find((rec) => rec._id === 'invite-to-accept');
    assert(invite);
    assert.equal(invite?.status, 'accepted');
    assert(invite?.acceptedAt instanceof Date);
    assert.equal(invite?.invitedUserId, usersCreated[0]?.id);
    assert.equal(invite?.tokenHash, null);
    assert.equal(personLinks[0]?.personId, 'person-77');
    assert.equal(personLinks[0]?.userId, usersCreated[0]?.id);
    assert.equal(auditLog[auditLog.length - 1]?.eventType, 'invite.accepted');
  });

  it('allows accept on legal hold (non-destructive) and clears token hash', async () => {
    const token = 'legal-hold';
    model = createInviteModel([
      {
        _id: 'invite-legal',
        orgId: 'org-1',
        email: 'hold@example.com',
        role: Role.User,
        tokenHash: hashToken(token),
        tokenExpires: new Date(Date.now() + 2 * 60 * 60 * 1000),
        status: 'pending',
        legalHold: true,
      },
    ]);
    const { service } = createService(model);

    const result = await service.accept({
      token,
      username: 'legal-user',
      password: 'P@ssw0rd!123',
    });

    assert.equal(result.user.email, 'hold@example.com');
    const invite = model.records.find((rec) => rec._id === 'invite-legal');
    assert(invite);
    assert.equal(invite?.status, 'accepted');
    assert.equal(invite?.tokenHash, null);
  });
});
