const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { Role } = require('../src/common/roles.ts');
const { InvitesController } = require('../src/features/invites/invites.controller.ts');

describe('InvitesController', () => {
  it('passes org, actor, and role from session to service on create', async () => {
    const calls: any[] = [];
    const service = {
      create: async (...args: any[]) => {
        calls.push(args);
        return { created: true };
      },
    };
    const controller = new InvitesController(service as any);
    const req: any = { session: { user: { orgId: 'org-1', id: 'actor-1', role: Role.Admin, roles: [Role.PM, Role.Admin] } } };
    const dto = { personId: 'person-1', role: Role.User };

    const result = await controller.create(dto as any, req);

    assert.deepEqual(result, { created: true });
    const [orgId, actorId, actorRole, passedDto] = calls[0];
    assert.equal(orgId, 'org-1');
    assert.equal(actorId, 'actor-1');
    assert.equal(actorRole, Role.Admin);
    assert.strictEqual(passedDto, dto);
  });

  it('resolves primary role from roles array when primary missing', async () => {
    const calls: any[] = [];
    const service = {
      create: async (...args: any[]) => {
        calls.push(args);
        return { created: true };
      },
    };
    const controller = new InvitesController(service as any);
    const req: any = { session: { user: { orgId: 'org-roles', id: 'actor-roles', roles: [Role.PlatformAdmin, Role.Viewer] } } };
    const dto = { personId: 'person-2', role: Role.Manager };

    await controller.create(dto as any, req);

    const [, , actorRole] = calls[0];
    assert.equal(actorRole, Role.PlatformAdmin);
  });

  it('forwards org and actor to resend', async () => {
    const calls: any[] = [];
    const service = {
      resend: async (...args: any[]) => {
        calls.push(args);
        return { resent: true };
      },
    };
    const controller = new InvitesController(service as any);
    const req: any = { session: { user: { orgId: 'org-2', id: 'actor-2', role: Role.OrgOwner } } };

    const result = await controller.resend('invite-1', req);

    assert.deepEqual(result, { resent: true });
    const [orgId, inviteId, actorId] = calls[0];
    assert.equal(orgId, 'org-2');
    assert.equal(inviteId, 'invite-1');
    assert.equal(actorId, 'actor-2');
  });

  it('delegates list to service using org from session', async () => {
    const calls: any[] = [];
    const service = {
      list: async (...args: any[]) => {
        calls.push(args);
        return [{ id: 'invite-1' }];
      },
    };
    const controller = new InvitesController(service as any);
    const req: any = { session: { user: { orgId: 'org-3', id: 'actor-3', role: Role.Admin } } };

    const result = await controller.list(req);

    assert.deepEqual(result, [{ id: 'invite-1' }]);
    assert.equal(calls[0][0], 'org-3');
  });

  it('delegates accept to service', async () => {
    let passed: any;
    const service = {
      accept: async (dto: any) => {
        passed = dto;
        return { ok: true };
      },
    };
    const controller = new InvitesController(service as any);
    const dto = { token: 't', username: 'u', password: 'P@ssw0rd!123' };

    const result = await controller.accept(dto as any);

    assert.deepEqual(result, { ok: true });
    assert.strictEqual(passed, dto);
  });
});
