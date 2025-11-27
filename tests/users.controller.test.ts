const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { ForbiddenException } = require('@nestjs/common');
const { UsersController } = require('../src/features/users/users.controller.ts');
const { Role } = require('../src/common/roles.ts');

const makeController = () => {
  const calls = {
    create: [],
    archive: [],
    unarchive: [],
    updateRoles: [],
    list: [],
    getById: [],
    update: [],
  };
  const service = {
    create: async (dto, actor) => {
      calls.create.push({ dto, actor });
      return { id: 'user-1', ...dto, createdBy: actor?.id };
    },
    archiveUser: async (id, actor) => {
      calls.archive.push({ id, actor });
      return { id, archivedAt: new Date(), actor };
    },
    unarchiveUser: async (id, actor) => {
      calls.unarchive.push({ id, actor });
      return { id, archivedAt: null, actor };
    },
    updateRoles: async (id, roles, actor) => {
      calls.updateRoles.push({ id, roles, actor });
      return { id, roles, actor };
    },
    list: async (actor, opts) => {
      calls.list.push({ actor, opts });
      return [{ id: 'listed', actor, opts }];
    },
    getById: async (id, actor, includeArchived) => {
      calls.getById.push({ id, actor, includeArchived });
      return { id, actor, includeArchived };
    },
    update: async (id, dto, actor) => {
      calls.update.push({ id, dto, actor });
      return { id, ...dto, actor };
    },
  };
  return { controller: new UsersController(service), calls };
};

describe('UsersController', () => {
  it('creates user using session org and preserves roles array', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { orgId: 'org-1', role: Role.Admin, roles: [Role.Admin, Role.PM] } } };
    const dto = { username: 'Jane', email: 'jane@example.com', password: 'Str0ng!Passw0rd', role: Role.User };

    const result = await controller.create(dto, req);

    assert.equal(calls.create.length, 1);
    assert.equal(calls.create[0].dto.organizationId, 'org-1');
    assert.deepEqual(calls.create[0].actor.roles, [Role.Admin, Role.PM]);
    assert.deepEqual(result.role, Role.User);
  });

  it('allows platform admin to create for specified org when session org is missing', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { role: Role.PlatformAdmin, roles: [Role.PlatformAdmin] } } };
    const dto = {
      username: 'Ops',
      email: 'ops@example.com',
      password: 'Str0ng!Passw0rd',
      organizationId: 'target-org',
      roles: [Role.OrgAdmin],
    };

    await controller.create(dto, req);

    assert.equal(calls.create[0].dto.organizationId, 'target-org');
  });

  it('throws when org is missing for non-privileged actor', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { role: Role.Admin, roles: [Role.Admin] } } };
    const dto = { username: 'NoOrg', email: 'noorg@example.com', password: 'Str0ng!Passw0rd' };

    let error: any;
    try {
      await controller.create(dto, req);
    } catch (err) {
      error = err;
    }
    assert.ok(error instanceof ForbiddenException);
    assert.equal(calls.create.length, 0);
  });

  it('archives and unarchives with actor roles forwarded', async () => {
    const { controller, calls } = makeController();
    const sessionUser = { orgId: 'org-9', role: Role.OrgOwner, roles: [Role.OrgOwner, Role.Manager] };
    const req = { session: { user: sessionUser } };

    await controller.archive('target-1', req);
    await controller.unarchive('target-1', req);

    assert.deepEqual(calls.archive[0].actor.roles, sessionUser.roles);
    assert.deepEqual(calls.unarchive[0].actor.roles, sessionUser.roles);
    assert.equal(calls.archive[0].actor.orgId, 'org-9');
  });

  it('updates roles with actor context and passes through role payload', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { orgId: 'org-2', role: Role.Admin, roles: [Role.Admin, Role.PM], id: 'actor-1' } } };
    const dto = { roles: [Role.Manager, Role.Viewer] };

    await controller.updateRoles('user-2', dto, req);

    assert.deepEqual(calls.updateRoles[0].roles, dto.roles);
    assert.equal(calls.updateRoles[0].actor.orgId, 'org-2');
    assert.deepEqual(calls.updateRoles[0].actor.roles, [Role.Admin, Role.PM]);
    assert.equal(calls.updateRoles[0].actor.id, 'actor-1');
  });

  it('lists users with includeArchived flag and optional org override', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { orgId: 'org-1', role: Role.Admin, roles: [Role.Admin] } } };

    const result = await controller.list({ includeArchived: true, organizationId: 'org-99' } as any, req as any);

    assert.equal(calls.list.length, 1);
    assert.equal(calls.list[0].opts.orgId, 'org-99');
    assert.equal(calls.list[0].opts.includeArchived, true);
    assert.deepEqual(calls.list[0].actor.roles, [Role.Admin]);
    assert.equal(result[0].id, 'listed');
  });

  it('gets user by id with includeArchived forwarded', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { orgId: 'org-1', role: Role.Admin, roles: [Role.Admin] } } };

    const result = await controller.getById('user-123', { includeArchived: true } as any, req as any);

    assert.equal(result.id, 'user-123');
    assert.equal(calls.getById[0].includeArchived, true);
    assert.deepEqual(calls.getById[0].actor.roles, [Role.Admin]);
  });

  it('updates a user with actor context', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { orgId: 'org-1', role: Role.OrgAdmin, roles: [Role.OrgAdmin], id: 'actor-7' } } };
    const dto = { username: 'Updated', piiStripped: true };

    const result = await controller.update('target-5', dto as any, req as any);

    assert.equal(result.username, 'Updated');
    assert.equal(calls.update[0].id, 'target-5');
    assert.deepEqual(calls.update[0].dto, dto);
    assert.equal(calls.update[0].actor.id, 'actor-7');
  });
});
