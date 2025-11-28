import { strict as assert } from 'node:assert';
import { after, before, beforeEach, describe, it } from 'node:test';
import request from 'supertest';
import { ForbiddenException, INestApplication, NotFoundException, ValidationPipe, BadRequestException } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { Reflector } from '@nestjs/core';
import { SessionGuard } from '../src/common/guards/session.guard.ts';
import { OrgScopeGuard } from '../src/common/guards/org-scope.guard.ts';
import { RolesGuard } from '../src/common/guards/roles.guard.ts';
import { Role, canAssignRoles, mergeRoles, resolvePrimaryRole } from '../src/common/roles.ts';
import { RbacController } from '../src/features/rbac/rbac.controller.ts';
import { RbacService } from '../src/features/rbac/rbac.service.ts';
import { UsersService } from '../src/features/users/users.service.ts';
import { ProjectsController } from '../src/features/projects/projects.controller.ts';
import { ProjectsService } from '../src/features/projects/projects.service.ts';

class FakeUsersService {
  audit: any[] = [];
  records: any[] = [];

  constructor() {
    this.reset();
  }

  reset() {
    this.audit.length = 0;
    this.records = [
      { id: 'u1', orgId: 'org-1', roles: [Role.Admin] },
      { id: 'u2', orgId: 'org-2', roles: [Role.Manager] },
      { id: 'u3', orgId: 'org-1', roles: [Role.User] },
    ];
  }

  private sanitize(user: any) {
    const roles = mergeRoles(user.role as Role, user.roles as Role[]);
    return { ...user, roles, role: user.role || resolvePrimaryRole(roles) };
  }

  private actorRoles(actor: any) {
    return mergeRoles(actor?.role as Role, actor?.roles as Role[]);
  }

  private isPrivileged(actorRoles: Role[]) {
    return actorRoles.includes(Role.SuperAdmin) || actorRoles.includes(Role.PlatformAdmin);
  }

  private assertScope(userOrgId: string | undefined, actor: any) {
    const actorRoles = this.actorRoles(actor);
    const privileged = this.isPrivileged(actorRoles);
    if (privileged) return;
    if (!actor?.orgId) {
      throw new ForbiddenException('Missing organization context');
    }
    if (userOrgId && actor.orgId !== userOrgId) {
      throw new ForbiddenException('Cannot access another organization');
    }
  }

  async listOrgRoles(orgId: string | undefined, actor: any) {
    const actorRoles = this.actorRoles(actor);
    const privileged = this.isPrivileged(actorRoles);
    const resolvedOrg = privileged && orgId ? orgId : actor?.orgId;
    if (!resolvedOrg) throw new ForbiddenException('Missing organization context');
    if (!privileged && orgId && orgId !== actor?.orgId) {
      throw new ForbiddenException('Cannot list another organization');
    }
    return this.records
      .filter((r) => r.orgId === resolvedOrg)
      .map((r) => this.sanitize(r));
  }

  async getUserRoles(id: string, actor: any) {
    const user = this.records.find((r) => r.id === id);
    if (!user) throw new NotFoundException('User not found');
    this.assertScope(user.orgId, actor);
    return this.sanitize(user);
  }

  async updateRoles(id: string, roles: Role[], actor: any) {
    const user = this.records.find((r) => r.id === id);
    if (!user) throw new NotFoundException('User not found');
    this.assertScope(user.orgId, actor);
    const normalized = mergeRoles(undefined, roles);
    if (!normalized.length) throw new BadRequestException('At least one role is required');
    const actorRoles = this.actorRoles(actor);
    if (!actorRoles.length) throw new ForbiddenException('Actor has no roles assigned');
    if (!canAssignRoles(actorRoles, normalized)) {
      throw new ForbiddenException('Insufficient role to assign requested roles');
    }
    user.roles = normalized;
    user.role = resolvePrimaryRole(normalized);
    this.audit.push({ eventType: 'user.roles_updated', roles: normalized, actorRoles });
    return this.sanitize(user);
  }
}

class FakeProjectsService {
  calls: any[] = [];

  reset() {
    this.calls.length = 0;
  }

  async create(dto: any, actor: any) {
    this.calls.push(['create', dto, actor]);
    return { id: 'p-1', orgId: actor.orgId, name: dto.name };
  }

  async list(actor: any, orgId: string) {
    this.calls.push(['list', actor, orgId]);
    return { data: [], orgId };
  }
}

describe('RBAC HTTP endpoints', () => {
  let app: INestApplication;
  const users = new FakeUsersService();
  const projects = new FakeProjectsService();

  const withUser = (user: any) => ({ 'x-test-user': JSON.stringify(user) });

  before(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [RbacController, ProjectsController],
      providers: [
        RbacService,
        SessionGuard,
        OrgScopeGuard,
        RolesGuard,
        Reflector,
        { provide: UsersService, useValue: users },
        { provide: ProjectsService, useValue: projects },
      ],
    }).compile();

    app = moduleRef.createNestApplication();
    app.use((req: any, _res: any, next: any) => {
      const header = req.headers['x-test-user'];
      if (header) {
        try {
          const parsed = JSON.parse(header as string);
          req.session = { user: parsed };
          req.user = parsed;
        } catch {
          // ignored; guards will deny
        }
      }
      next();
    });
    app.useGlobalPipes(new ValidationPipe({ transform: true, whitelist: true }));
    await app.init();
  });

  beforeEach(() => {
    users.reset();
    projects.reset();
  });

  after(async () => {
    await app.close();
  });

  it('allows platform admin to list another org', async () => {
    const res = await request(app.getHttpServer())
      .get('/rbac/users?organizationId=org-2')
      .set(withUser({ id: 'actor', role: Role.PlatformAdmin, roles: [Role.PlatformAdmin] }))
      .expect(200);

    assert.equal(res.body.length, 1);
    assert.equal(res.body[0].orgId, 'org-2');
  });

  it('blocks cross-org listing for org admin', async () => {
    await request(app.getHttpServer())
      .get('/rbac/users?organizationId=org-2')
      .set(withUser({ id: 'actor', orgId: 'org-1', role: Role.Admin, roles: [Role.Admin] }))
      .expect(403);
  });

  it('prevents assigning higher role than actor via PATCH', async () => {
    await request(app.getHttpServer())
      .patch('/rbac/users/u3/roles')
      .set(withUser({ id: 'actor', orgId: 'org-1', role: Role.Admin, roles: [Role.Admin] }))
      .send({ roles: [Role.OrgOwner] })
      .expect(403);

    const target = users.records.find((r) => r.id === 'u3');
    assert.deepEqual(target?.roles, [Role.User]);
  });

  it('rejects empty roles payload through validation', async () => {
    await request(app.getHttpServer())
      .patch('/rbac/users/u3/roles')
      .set(withUser({ id: 'actor', orgId: 'org-1', role: Role.SuperAdmin }))
      .send({ roles: [] })
      .expect(400);
  });

  it('rejects revoking the last remaining role', async () => {
    await request(app.getHttpServer())
      .delete('/rbac/users/u1/roles/admin')
      .set(withUser({ id: 'actor', role: Role.SuperAdmin }))
      .expect(400);
  });

  it('enforces RolesGuard + OrgScopeGuard on guarded CRUD routes', async () => {
    await request(app.getHttpServer())
      .post('/projects')
      .set(withUser({ id: 'viewer-1', role: Role.Viewer, orgId: 'org-1', roles: [Role.Viewer] }))
      .send({ name: 'Forbidden Project' })
      .expect(403);
    assert.equal(projects.calls.length, 0);

    const res = await request(app.getHttpServer())
      .post('/projects')
      .set(withUser({ id: 'super-1', role: Role.SuperAdmin }))
      .send({ name: 'Privileged Project' })
      .expect(201);

    assert.equal(res.body.name, 'Privileged Project');
    const createCall = projects.calls.find(([action]) => action === 'create');
    assert.ok(createCall);
    assert.equal(createCall[2].role, Role.SuperAdmin);
  });

  it('requires org context for non-privileged users on scoped routes', async () => {
    await request(app.getHttpServer())
      .get('/projects')
      .set(withUser({ id: 'manager-1', role: Role.Manager, roles: [Role.Manager] }))
      .expect(403);

    assert.equal(projects.calls.length, 0);
  });
});
