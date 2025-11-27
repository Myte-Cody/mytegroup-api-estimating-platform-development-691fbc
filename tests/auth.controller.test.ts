const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { AuthController } = require('../src/features/auth/auth.controller.ts');
const { Role } = require('../src/common/roles.ts');

describe('AuthController', () => {
  it('register sets session user with roles array and org context', async () => {
    const calls = [];
    const controller = new AuthController(
      {
        register: async (dto) => {
          calls.push(dto);
          return {
            id: 'user-reg',
            role: Role.OrgOwner,
            roles: [Role.OrgOwner, Role.Admin],
            organizationId: 'org-1',
            isOrgOwner: true,
          };
        },
      } as any,
      {
        acceptanceStatus: async () => ({ required: [], acceptedVersions: {}, latest: [] }),
      } as any
    );
    const req: any = { session: {} };
    const dto = { email: 'owner@example.com', password: 'Str0ng!Passw0rd', username: 'Owner', organizationName: 'Org 1' };

    const result = await controller.register(dto as any, req);

    assert.deepEqual(calls[0], dto);
    assert.equal(result.user.id, 'user-reg');
    assert.equal(req.session.user.orgId, 'org-1');
    assert.deepEqual(req.session.user.roles, [Role.OrgOwner, Role.Admin]);
    assert.equal(req.session.user.role, Role.OrgOwner);
    assert.equal(req.session.user.isOrgOwner, true);
  });

  it('login stores session user and preserves returned roles array', async () => {
    const calls = [];
    const controller = new AuthController(
      {
        login: async (email, password) => {
          calls.push({ email, password });
          return {
            _id: 'user-login',
            id: 'user-login',
            role: Role.PlatformAdmin,
            roles: [Role.PlatformAdmin, Role.SuperAdmin],
            organizationId: 'org-9',
          };
        },
      } as any,
      {
        acceptanceStatus: async () => ({ required: [], acceptedVersions: {}, latest: [] }),
      } as any
    );
    const req: any = { session: {} };
    const body = { email: 'pa@example.com', password: 'Secr3t!Pass123' };

    const result = await controller.login(body as any, req);

    assert.deepEqual(calls[0], body);
    assert.equal(result.user.id, 'user-login');
    assert.equal(req.session.user.id, 'user-login');
    assert.equal(req.session.user.orgId, 'org-9');
    assert.deepEqual(req.session.user.roles, [Role.PlatformAdmin, Role.SuperAdmin]);
    assert.equal(req.session.user.role, Role.PlatformAdmin);
  });

  it('login falls back to primary role when roles array missing', async () => {
    const controller = new AuthController(
      {
        login: async () => ({ id: 'user-one', role: Role.User, organizationId: 'org-2' }),
      } as any,
      { acceptanceStatus: async () => ({ required: [], acceptedVersions: {}, latest: [] }) } as any
    );
    const req: any = { session: {} };

    await controller.login({ email: 'u@example.com', password: 'pass' } as any, req);

    assert.deepEqual(req.session.user.roles, [Role.User]);
    assert.equal(req.session.user.orgId, 'org-2');
  });

  it('logout destroys the session and returns ok', async () => {
    let destroyed = false;
    const controller = new AuthController({} as any, { acceptanceStatus: async () => ({ required: [], acceptedVersions: {}, latest: [] }) } as any);
    const req: any = { session: { destroy: (cb: () => void) => { destroyed = true; cb(); } } };

    const result = await controller.logout(req);

    assert.equal(result.status, 'ok');
    assert.equal(destroyed, true);
  });

  it('returns the session snapshot from me()', () => {
    const controller = new AuthController({} as any, { acceptanceStatus: async () => ({ required: [], acceptedVersions: {}, latest: [] }) } as any);
    const sessionUser = { id: 'me', orgId: 'org-3', roles: [Role.Manager] };

    const result = controller.me({ session: { user: sessionUser } } as any);

    assert.deepEqual(result.user, sessionUser);
  });

  it('delegates token/verification flows to the service', async () => {
    const calls = { verifyEmail: [], forgotPassword: [], resetPassword: [], passwordStrength: [] };
    const controller = new AuthController(
      {
        verifyEmail: async (dto) => {
          calls.verifyEmail.push(dto);
          return { verified: true };
        },
        forgotPassword: async (dto) => {
          calls.forgotPassword.push(dto);
          return { sent: true };
        },
        resetPassword: async (dto) => {
          calls.resetPassword.push(dto);
          return { reset: true };
        },
        passwordStrength: async (password) => {
          calls.passwordStrength.push(password);
          return { score: 4 };
        },
      } as any,
      { acceptanceStatus: async () => ({ required: [], acceptedVersions: {}, latest: [] }) } as any
    );

    const verify = await controller.verifyEmail({ token: 'tok-1' } as any);
    const strength = await controller.passwordStrength({ password: 'P@ssw0rd!123' } as any);
    const forgot = await controller.forgotPassword({ email: 'user@example.com' } as any);
    const reset = await controller.resetPassword({ token: 'tok-2', newPassword: 'An0ther$Pass' } as any);

    assert.equal(calls.verifyEmail[0].token, 'tok-1');
    assert.equal(calls.passwordStrength[0], 'P@ssw0rd!123');
    assert.deepEqual(calls.forgotPassword[0], { email: 'user@example.com' });
    assert.deepEqual(calls.resetPassword[0], { token: 'tok-2', newPassword: 'An0ther$Pass' });
    assert.deepEqual(verify, { verified: true });
    assert.deepEqual(strength, { score: 4 });
    assert.deepEqual(forgot, { sent: true });
    assert.deepEqual(reset, { reset: true });
  });

  it('listUsers forwards organization context to the service', async () => {
    const calls = [];
    const controller = new AuthController(
      {
        listUsers: async (orgId, actor) => {
          calls.push({ orgId, actor });
          return [{ id: 'user-1' }];
        },
      } as any,
      { acceptanceStatus: async () => ({ required: [], acceptedVersions: {}, latest: [] }) } as any
    );
    const sessionUser = { orgId: 'org-5', roles: [Role.Admin], role: Role.Admin };
    const req: any = { session: { user: sessionUser } };

    const users = await controller.listUsers(req);

    assert.equal(calls[0].orgId, 'org-5');
    assert.deepEqual(calls[0].actor, sessionUser);
    assert.deepEqual(users, [{ id: 'user-1' }]);
  });
});
