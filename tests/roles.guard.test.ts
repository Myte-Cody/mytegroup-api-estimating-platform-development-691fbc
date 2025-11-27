require('reflect-metadata');
const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { Reflector } = require('@nestjs/core');
const { RolesGuard } = require('../src/common/guards/roles.guard.ts');
const { Role } = require('../src/common/roles.ts');

const createContext = (requiredRoles: Role[], user: any) => {
  const reflector = new Reflector();
  const handler = () => null;
  Reflect.defineMetadata('roles', requiredRoles, handler);
  const request = { user, session: { user } };
  const context: any = {
    getHandler: () => handler,
    getClass: () => handler,
    switchToHttp: () => ({ getRequest: () => request }),
  };
  const guard = new RolesGuard(reflector as any);
  return { guard, context };
};

describe('RolesGuard', () => {
  it('allows when user has matching role from roles array', () => {
    const user = { roles: [Role.PM, Role.Viewer], orgId: 'org-1' };
    const { guard, context } = createContext([Role.PM], user);
    assert.equal(guard.canActivate(context as any), true);
  });

  it('allows hierarchical access (org owner satisfies admin)', () => {
    const user = { roles: [Role.OrgOwner], orgId: 'org-1' };
    const { guard, context } = createContext([Role.Admin], user);
    assert.equal(guard.canActivate(context as any), true);
  });

  it('denies when org context missing for non-privileged user', () => {
    const user = { role: Role.Admin };
    const { guard, context } = createContext([Role.Admin], user);
    assert.equal(guard.canActivate(context as any), false);
  });

  it('allows platform admin without org context', () => {
    const user = { roles: [Role.PlatformAdmin] };
    const { guard, context } = createContext([Role.Admin], user);
    assert.equal(guard.canActivate(context as any), true);
  });

  it('denies when user lacks required role', () => {
    const user = { roles: [Role.Viewer], orgId: 'org-1' };
    const { guard, context } = createContext([Role.Admin], user);
    assert.equal(guard.canActivate(context as any), false);
  });

  it('uses roles array when primary role missing', () => {
    const user = { roles: [Role.Manager], orgId: 'org-1' };
    const { guard, context } = createContext([Role.Manager], user);
    assert.equal(guard.canActivate(context as any), true);
  });
});
