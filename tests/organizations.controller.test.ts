const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { ForbiddenException } = require('@nestjs/common');
const { OrganizationsController } = require('../src/features/organizations/organizations.controller.ts');
const { Role } = require('../src/common/roles.ts');

describe('OrganizationsController', () => {
  it('passes actor to service on create', async () => {
    const calls: any[] = [];
    const service = { create: async (...args: any[]) => calls.push(args) };
    const controller = new OrganizationsController(service as any);
    const req: any = { session: { user: { id: 'actor-1', role: Role.SuperAdmin } } };
    const dto = { name: 'New Org' };

    await controller.create(dto as any, req);

    assert.equal(calls.length, 1);
    assert.deepEqual(calls[0][0], dto);
    assert.equal(calls[0][1].id, 'actor-1');
  });

  it('blocks access to other orgs for non-superadmin on read', async () => {
    let called = false;
    const service = {
      findById: async () => {
        called = true;
        return {};
      },
    };
    const controller = new OrganizationsController(service as any);
    const req: any = { session: { user: { orgId: 'org-1', role: Role.Admin } } };

    await assert.rejects(() => controller.findOne('org-2', req), ForbiddenException);
    assert.equal(called, false);
  });

  it('delegates update to service for same-org admin', async () => {
    const calls: any[] = [];
    const service = {
      update: async (...args: any[]) => {
        calls.push(args);
        return { ok: true };
      },
    };
    const controller = new OrganizationsController(service as any);
    const req: any = { session: { user: { orgId: 'org-1', role: Role.Admin, id: 'actor-2' } } };
    const dto = { name: 'Renamed' };

    const result = await controller.update('org-1', dto as any, req);

    assert.deepEqual(result, { ok: true });
    assert.equal(calls[0][0], 'org-1');
    assert.strictEqual(calls[0][1], dto);
    assert.equal(calls[0][2].id, 'actor-2');
  });

  it('passes actor through for datastore, legal hold, and PII toggles', async () => {
    const calls: any[] = [];
    const service = {
      updateDatastore: async (...args: any[]) => {
        calls.push(['datastore', ...args]);
        return { datastore: true };
      },
      setLegalHold: async (...args: any[]) => {
        calls.push(['legalHold', ...args]);
        return { legal: true };
      },
      setPiiStripped: async (...args: any[]) => {
        calls.push(['pii', ...args]);
        return { pii: true };
      },
    };
    const controller = new OrganizationsController(service as any);
    const req: any = { session: { user: { id: 'actor-3', role: Role.SuperAdmin } } };

    await controller.updateDatastore('org-9', { type: 'shared' } as any, req);
  await controller.setLegalHold('org-9', { legalHold: true } as any, req);
  await controller.setPiiStripped('org-9', { piiStripped: true } as any, req);

  const datastoreCall = calls.find((c) => c[0] === 'datastore');
  const legalCall = calls.find((c) => c[0] === 'legalHold');
  const piiCall = calls.find((c) => c[0] === 'pii');

  assert.equal(datastoreCall[3].id, 'actor-3');
  assert.equal(legalCall[3].id, 'actor-3');
  assert.equal(piiCall[3].id, 'actor-3');
});
});
