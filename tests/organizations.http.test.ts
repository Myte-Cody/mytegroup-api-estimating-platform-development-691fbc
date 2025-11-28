import { strict as assert } from 'node:assert';
import { after, before, describe, it } from 'node:test';
import request from 'supertest';
import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { Reflector } from '@nestjs/core';
import { OrganizationsController } from '../src/features/organizations/organizations.controller.ts';
import { OrganizationsService } from '../src/features/organizations/organizations.service.ts';
import { SessionGuard } from '../src/common/guards/session.guard.ts';
import { RolesGuard } from '../src/common/guards/roles.guard.ts';
import { Role } from '../src/common/roles.ts';

describe('OrganizationsController HTTP', () => {
  let app: INestApplication;
  const calls: any[] = [];
  const service: any = {
    create: async (dto: any, actor: any) => {
      calls.push(['create', dto, actor]);
      return { id: 'org-123', name: dto.name, metadata: dto.metadata || {}, datastoreType: dto.datastoreType || 'shared' };
    },
    findById: async (id: string) => {
      calls.push(['findById', id]);
      return { id, name: 'Existing Org' };
    },
    update: async (id: string, dto: any, actor: any) => {
      calls.push(['update', id, dto, actor]);
      return { id, ...dto };
    },
    archive: async (id: string, actor: any) => {
      calls.push(['archive', id, actor]);
      return { id, archivedAt: new Date('2024-01-01').toISOString() };
    },
    unarchive: async (id: string, actor: any) => {
      calls.push(['unarchive', id, actor]);
      return { id, archivedAt: null };
    },
    updateDatastore: async (id: string, dto: any, actor: any) => {
      calls.push(['datastore', id, dto, actor]);
      return { id, datastoreType: dto.type, datastoreUri: dto.uri || null };
    },
    setLegalHold: async (id: string, dto: any, actor: any) => {
      calls.push(['legalHold', id, dto, actor]);
      return { id, legalHold: dto.legalHold };
    },
    setPiiStripped: async (id: string, dto: any, actor: any) => {
      calls.push(['pii', id, dto, actor]);
      return { id, piiStripped: dto.piiStripped };
    },
  };

  const withUser = (user: any) => ({ 'x-test-user': JSON.stringify(user) });

  before(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [OrganizationsController],
      providers: [
        { provide: OrganizationsService, useValue: service },
        SessionGuard,
        RolesGuard,
        Reflector,
      ],
    }).compile();

    app = moduleRef.createNestApplication();
    app.use((req: any, _res: any, next: any) => {
      const header = req.headers['x-test-user'] as string | undefined;
      if (header) {
        try {
          const parsed = JSON.parse(header);
          req.session = { user: parsed };
          req.user = parsed;
        } catch {
          // ignore parse errors and let guards deny
        }
      }
      next();
    });
    await app.init();
  });

  after(async () => {
    await app.close();
  });

  it('allows superadmin to create and passes actor', async () => {
    calls.length = 0;
    const res = await request(app.getHttpServer())
      .post('/organizations')
      .set(withUser({ id: 'super-1', role: Role.SuperAdmin }))
      .send({ name: 'Acme Corp' })
      .expect(201);

    assert.equal(res.body.id, 'org-123');
    const call = calls.find(([action]) => action === 'create');
    assert.ok(call);
    assert.equal(call[1].name, 'Acme Corp');
    assert.equal(call[2].id, 'super-1');
  });

  it('blocks admins from accessing other orgs and does not hit service', async () => {
    calls.length = 0;
    await request(app.getHttpServer())
      .get('/organizations/org-2')
      .set(withUser({ id: 'user-1', role: Role.Admin, orgId: 'org-1', roles: [Role.Admin] }))
      .expect(403);

    const call = calls.find(([action]) => action === 'findById');
    assert.equal(call, undefined);
  });

  it('allows same-org owner to update metadata and forwards actor', async () => {
    calls.length = 0;
    const res = await request(app.getHttpServer())
      .patch('/organizations/org-1')
      .set(withUser({ id: 'owner-1', role: Role.OrgOwner, orgId: 'org-1', roles: [Role.OrgOwner] }))
      .send({ name: 'Renamed Org', metadata: { tier: 'gold' } })
      .expect(200);

    assert.equal(res.body.name, 'Renamed Org');
    const call = calls.find(([action]) => action === 'update');
    assert.ok(call);
    assert.equal(call[1], 'org-1');
    assert.equal(call[2].metadata.tier, 'gold');
    assert.equal(call[3].id, 'owner-1');
  });

  it('enforces superadmin on datastore switch', async () => {
    calls.length = 0;
    await request(app.getHttpServer())
      .patch('/organizations/org-1/datastore')
      .set(withUser({ id: 'owner-2', role: Role.OrgOwner, orgId: 'org-1', roles: [Role.OrgOwner] }))
      .send({ type: 'dedicated', uri: 'mongodb://tenant' })
      .expect(403);

    assert.equal(calls.find(([action]) => action === 'datastore'), undefined);

    const res = await request(app.getHttpServer())
      .patch('/organizations/org-1/datastore')
      .set(withUser({ id: 'super-2', role: Role.SuperAdmin }))
      .send({ type: 'dedicated', uri: 'mongodb://tenant' })
      .expect(200);

    assert.equal(res.body.datastoreType, 'dedicated');
    const call = calls.find(([action]) => action === 'datastore');
    assert.ok(call);
    assert.equal(call[3].id, 'super-2');
    assert.equal(call[2].uri, 'mongodb://tenant');
  });

  it('allows compliance toggles for superadmin', async () => {
    calls.length = 0;
    await request(app.getHttpServer())
      .post('/organizations/org-1/legal-hold')
      .set(withUser({ id: 'super-3', role: Role.SuperAdmin }))
      .send({ legalHold: true })
      .expect(201);

    await request(app.getHttpServer())
      .post('/organizations/org-1/pii-stripped')
      .set(withUser({ id: 'super-3', role: Role.SuperAdmin }))
      .send({ piiStripped: true })
      .expect(201);

    const legalHoldCall = calls.find(([action]) => action === 'legalHold');
    const piiCall = calls.find(([action]) => action === 'pii');
    assert.ok(legalHoldCall);
    assert.ok(piiCall);
    assert.equal(legalHoldCall[3].id, 'super-3');
    assert.equal(piiCall[3].id, 'super-3');
  });
});
