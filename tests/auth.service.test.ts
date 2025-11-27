const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const bcrypt = require('bcryptjs');
const crypto = require('node:crypto');
const { ForbiddenException, BadRequestException } = require('@nestjs/common');
const { AuthService } = require('../src/features/auth/auth.service.ts');
const { STRONG_PASSWORD_REGEX } = require('../src/features/auth/dto/password-rules.ts');
const { Role } = require('../src/common/roles.ts');

const hashPassword = (password) => bcrypt.hashSync(password, 4);

const makeDeps = (overrides = {}) => {
  const auditEvents = [];
  const audit = { log: async (evt) => auditEvents.push(evt) };
  const users = {
    findByEmail: async () => null,
    markLastLogin: async () => null,
    findByResetToken: async () => null,
    clearResetTokenAndSetPassword: async () => null,
    findByVerificationToken: async () => null,
    clearVerificationToken: async () => null,
    setResetToken: async () => null,
    ...overrides.users,
  };
  const orgs = {
    create: async () => ({ id: 'org-new' }),
    findById: async () => ({ id: 'org-new' }),
    setOwner: async () => null,
    ...overrides.orgs,
  };
  const email = {
    sendVerificationEmail: async () => null,
    sendPasswordResetEmail: async () => null,
    ...overrides.email,
  };
  const svc = new AuthService(audit, users, orgs, email);
  return { svc, auditEvents, users };
};

describe('AuthService login and tokens', () => {
  it('logs in active/verified user and updates lastLogin', async () => {
    const password = 'Str0ng!Passw0rd';
    const hash = hashPassword(password);
    const lastLogin = new Date();
    const { svc, auditEvents } = makeDeps({
      users: {
        findByEmail: async () => ({
          id: 'u1',
          organizationId: 'org1',
          role: Role.User,
          roles: [Role.User],
          passwordHash: hash,
          archivedAt: null,
          legalHold: false,
          isEmailVerified: true,
        }),
        markLastLogin: async (id) => ({
          id,
          organizationId: 'org1',
          role: Role.User,
          roles: [Role.User],
          passwordHash: hash,
          lastLogin,
        }),
      },
    });

    const result = await svc.login('test@example.com', password);

    assert.equal(result.id, 'u1');
    assert.ok(!result.passwordHash);
    assert.equal(result.lastLogin, lastLogin);
    assert.equal(auditEvents[0].eventType, 'auth.login');
    assert.equal(auditEvents[0].metadata.success, true);
  });

  it('rejects archived, legalHold, and unverified users', async () => {
    const password = 'Str0ng!Passw0rd';
    const hash = hashPassword(password);
    const baseUser = {
      id: 'u2',
      organizationId: 'org2',
      role: Role.User,
      roles: [Role.User],
      passwordHash: hash,
      isEmailVerified: true,
      legalHold: false,
      archivedAt: null,
    };
    const table = [
      { archivedAt: new Date(), legalHold: false, isEmailVerified: true },
      { archivedAt: null, legalHold: true, isEmailVerified: true },
      { archivedAt: null, legalHold: false, isEmailVerified: false },
    ];

    for (const state of table) {
      const { svc } = makeDeps({
        users: { findByEmail: async () => ({ ...baseUser, ...state }) },
      });
      await assert.rejects(() => svc.login('user@example.com', password), ForbiddenException);
    }
  });

  it('fails reset password on invalid or expired token', async () => {
    const { svc } = makeDeps({
      users: { findByResetToken: async () => null },
    });
    await assert.rejects(
      () => svc.resetPassword({ token: 'missing', newPassword: 'Another$Pass123' }),
      BadRequestException
    );
  });

  it('fails reset when user on legal hold', async () => {
    const { svc } = makeDeps({
      users: {
        findByResetToken: async () => ({
          id: 'u3',
          organizationId: 'org3',
          role: Role.User,
          roles: [Role.User],
          legalHold: true,
          archivedAt: null,
          passwordHash: hashPassword('Original$Pass123'),
        }),
      },
    });
    await assert.rejects(
      () => svc.resetPassword({ token: 'held-token', newPassword: 'Another$Pass123' }),
      ForbiddenException
    );
  });

  it('resets password and emits audit for valid token', async () => {
    const resetToken = 'tok-123';
    const resetHash = crypto.createHash('sha256').update(resetToken).digest('hex');
    let clearCalled = false;
    const { svc, auditEvents } = makeDeps({
      users: {
        findByResetToken: async (hash) => {
          assert.equal(hash, resetHash);
          return {
            id: 'u4',
            organizationId: 'org4',
            role: Role.User,
            roles: [Role.User],
            legalHold: false,
            archivedAt: null,
            passwordHash: hashPassword('Old$Pass123'),
          };
        },
        clearResetTokenAndSetPassword: async (id, newPassword) => {
          clearCalled = true;
          assert.ok(STRONG_PASSWORD_REGEX.test(newPassword));
          return { id, organizationId: 'org4', role: Role.User, roles: [Role.User] };
        },
      },
    });

    const result = await svc.resetPassword({ token: resetToken, newPassword: 'Another$Pass123' });

    assert.equal(result.id, 'u4');
    assert.ok(clearCalled);
    assert.equal(auditEvents.at(-1).eventType, 'auth.password_reset');
  });

  it('verifies email with valid token and audits', async () => {
    const token = 'verify-abc';
    const hash = crypto.createHash('sha256').update(token).digest('hex');
    let cleared = false;
    const { svc, auditEvents } = makeDeps({
      users: {
        findByVerificationToken: async (incomingHash) => {
          assert.equal(incomingHash, hash);
          return { id: 'u5', organizationId: 'org5', role: Role.User, roles: [Role.User], archivedAt: null };
        },
        clearVerificationToken: async () => {
          cleared = true;
          return { id: 'u5', organizationId: 'org5' };
        },
      },
    });

    const result = await svc.verifyEmail({ token });

    assert.equal(result.id, 'u5');
    assert.ok(cleared);
    assert.equal(auditEvents.at(-1).eventType, 'auth.verify_email');
  });

  it('rejects verification for invalid token', async () => {
    const { svc } = makeDeps({
      users: { findByVerificationToken: async () => null },
    });
    await assert.rejects(() => svc.verifyEmail({ token: 'bad' }), BadRequestException);
  });
});
