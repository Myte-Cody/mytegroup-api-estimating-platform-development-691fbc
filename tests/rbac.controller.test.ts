const { describe, it, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { ForbiddenException, BadRequestException, NotFoundException } = require('@nestjs/common');
const { Role, canAssignRoles, mergeRoles, resolvePrimaryRole } = require('../src/common/roles.ts');
const { RbacController } = require('../src/features/rbac/rbac.controller.ts');
const { RbacService } = require('../src/features/rbac/rbac.service.ts');

class FakeUsersService {
  constructor() {
    this.auditLog = [];
    this.reset();
  }

  reset() {
    this.auditLog.length = 0;
    this.records = [
      { id: 'u1', orgId: 'org-1', roles: [Role.User] },
      { id: 'u2', orgId: 'org-1', roles: [Role.Admin] },
      { id: 'u3', orgId: 'org-2', roles: [Role.User] },
    ];
  }

  sanitize(user) {
    const roles = mergeRoles(user.role, user.roles);
    return { id: user.id, orgId: user.orgId, roles, role: user.role || resolvePrimaryRole(roles) };
  }

  findRecord(id) {
    return this.records.find((r) => r.id === id);
  }

  getActorRoles(actor) {
    return mergeRoles(actor?.role, actor?.roles);
  }

  isPrivileged(actorRoles) {
    return actorRoles.includes(Role.SuperAdmin) || actorRoles.includes(Role.PlatformAdmin);
  }

  listOrgRoles(orgId, actor) {
    const actorRoles = this.getActorRoles(actor);
    if (!actorRoles.length) throw new ForbiddenException('Missing actor roles');
    const privileged = this.isPrivileged(actorRoles);
    const resolvedOrg = privileged && orgId ? orgId : actor?.orgId;
    if (!resolvedOrg) throw new ForbiddenException('Missing organization context');
    if (!privileged && orgId && orgId !== actor?.orgId) {
      throw new ForbiddenException('Cannot list another organization');
    }
    return this.records.filter((r) => r.orgId === resolvedOrg).map((r) => this.sanitize(r));
  }

  getUserRoles(userId, actor) {
    const user = this.findRecord(userId);
    if (!user) throw new NotFoundException('User not found');
    const actorRoles = this.getActorRoles(actor);
    const privileged = this.isPrivileged(actorRoles);
    if (!privileged && actor?.orgId !== user.orgId) {
      throw new ForbiddenException('Cross-org denied');
    }
    return this.sanitize(user);
  }

  updateRoles(userId, roles, actor) {
    const user = this.findRecord(userId);
    if (!user) throw new NotFoundException('User not found');
    const normalizedRoles = mergeRoles(undefined, roles);
    if (!normalizedRoles.length) throw new BadRequestException('At least one role');
    const actorRoles = this.getActorRoles(actor);
    if (!actorRoles.length) throw new ForbiddenException('Missing actor roles');
    const privileged = this.isPrivileged(actorRoles);
    if (!privileged && actor?.orgId !== user.orgId) {
      throw new ForbiddenException('Cross-org denied');
    }
    if (!canAssignRoles(actorRoles, normalizedRoles)) {
      throw new ForbiddenException('Insufficient role to assign');
    }
    user.roles = normalizedRoles;
    user.role = resolvePrimaryRole(normalizedRoles);
    this.auditLog.push({ eventType: 'user.roles_updated', roles: normalizedRoles, actorRoles });
    return this.sanitize(user);
  }
}

describe('RbacController', () => {
  let users;
  let controller;

  beforeEach(() => {
    users = new FakeUsersService();
    controller = new RbacController(new RbacService(users));
  });

  it('returns hierarchy from service', () => {
    const res = controller.hierarchy();
    assert.ok(res.roles.includes(Role.SuperAdmin));
    assert.ok(res.priority.length > 0);
  });

  it('blocks cross-org listing for non-privileged actors', async () => {
    const req = { session: { user: { orgId: 'org-1', roles: [Role.Admin] } }, query: { organizationId: 'org-2' } };
    await assert.rejects(() => controller.list(req, 'org-2'), ForbiddenException);
  });

  it('allows platform admin to list other orgs', async () => {
    const req = { session: { user: { roles: [Role.PlatformAdmin] } }, query: { organizationId: 'org-2' } };
    const res = await controller.list(req, 'org-2');
    assert.equal(res.length, 1);
    assert.equal(res[0].orgId, 'org-2');
  });

  it('assigns roles and records audit', async () => {
    const req = { session: { user: { roles: [Role.SuperAdmin], orgId: 'org-1', id: 'actor-1' } } };
    const dto = { roles: [Role.Manager, Role.Viewer] };
    const result = await controller.updateRoles('u1', dto, req);
    assert.deepEqual(new Set(result.roles), new Set([Role.Manager, Role.Viewer]));
    assert.equal(users.auditLog.at(-1)?.eventType, 'user.roles_updated');
  });

  it('prevents assigning higher role than actor', async () => {
    const req = { session: { user: { roles: [Role.Admin], orgId: 'org-1', id: 'actor-2' } } };
    const dto = { roles: [Role.OrgOwner] };
    await assert.rejects(() => controller.updateRoles('u1', dto, req), ForbiddenException);
  });

  it('rejects revoking the last remaining role', async () => {
    const req = { session: { user: { roles: [Role.SuperAdmin] } } };
    await assert.rejects(() => controller.revokeRole('u2', Role.Admin, req), BadRequestException);
  });

  it('allows revoking a role when another remains', async () => {
    const req = { session: { user: { roles: [Role.SuperAdmin] } } };
    await controller.updateRoles('u2', { roles: [Role.Admin, Role.Viewer] }, req);
    const updated = await controller.revokeRole('u2', Role.Viewer, req);
    assert.deepEqual(new Set(updated.roles), new Set([Role.Admin]));
  });
});
