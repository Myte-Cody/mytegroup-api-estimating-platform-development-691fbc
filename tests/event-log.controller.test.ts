const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { ForbiddenException } = require('@nestjs/common');
const { EventLogController } = require('../src/common/events/event-log.controller.ts');
const { Role } = require('../src/common/roles.ts');

const makeController = () => {
  const calls = {
    list: [],
    getOne: [],
  };
  const service = {
    list: async (...args) => {
      calls.list.push(args);
      return { data: [], total: 0 };
    },
    findByIdScoped: async (...args) => {
      calls.getOne.push(args);
      return { id: args[0], orgId: args[1] };
    },
  };
  return { controller: new EventLogController(service), calls };
};

describe('EventLogController', () => {
  it('uses session org for admins and forwards query unchanged', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { orgId: 'org-1', role: Role.Admin } } };
    const query = { entityType: 'Project', limit: 10 };

    const result = await controller.list(query, req);

    assert.deepEqual(result, { data: [], total: 0 });
    const [orgArg, queryArg, roleArg] = calls.list[0];
    assert.equal(orgArg, 'org-1');
    assert.strictEqual(queryArg, query);
    assert.equal(roleArg, Role.Admin);
  });

  it('requires org for super admin and allows explicit override', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { role: Role.SuperAdmin } } };
    const query = { orgId: 'target-org', limit: 5 };

    const result = await controller.list(query, req);
    assert.equal(calls.list[0][0], 'target-org');
    assert.strictEqual(result.total, 0);

    let error;
    try {
      await controller.list({}, req);
    } catch (err) {
      error = err;
    }
    assert.ok(error instanceof ForbiddenException);
  });

  it('rejects org owners from listing audit logs', async () => {
    const { controller } = makeController();
    const req = { session: { user: { orgId: 'org-1', role: Role.OrgOwner } } };
    await assert.rejects(() => controller.list({}, req), /Insufficient role/);
  });

  it('getOne scopes to org and uses requested org for super admin', async () => {
    const { controller, calls } = makeController();
    const req = { session: { user: { role: Role.SuperAdmin } } };

    const result = await controller.getOne('event-1', 'org-9', req);

    const [idArg, orgArg, roleArg, requestedOrgArg] = calls.getOne[0];
    assert.equal(idArg, 'event-1');
    assert.equal(orgArg, 'org-9');
    assert.equal(requestedOrgArg, 'org-9');
    assert.equal(roleArg, Role.SuperAdmin);
    assert.equal(result.id, 'event-1');
  });

  it('requires org on getOne for non-superadmin', async () => {
    const { controller } = makeController();
    const req = { session: { user: { role: Role.Admin } } };

    await assert.rejects(() => controller.getOne('event-2', undefined, req), /Organization context required/);
  });
});
