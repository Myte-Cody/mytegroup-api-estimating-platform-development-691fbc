import { strict as assert } from 'node:assert';
import { after, before, beforeEach, describe, it } from 'node:test';
import * as request from 'supertest';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { Reflector } from '@nestjs/core';
import { SessionGuard } from '../src/common/guards/session.guard.ts';
import { OrgScopeGuard } from '../src/common/guards/org-scope.guard.ts';
import { RolesGuard } from '../src/common/guards/roles.guard.ts';
import { Role } from '../src/common/roles.ts';
import { ComplianceController } from '../src/features/compliance/compliance.controller.ts';
import { ComplianceService } from '../src/features/compliance/compliance.service.ts';
import { BulkController } from '../src/features/bulk/bulk.controller.ts';
import { BulkService } from '../src/features/bulk/bulk.service.ts';

class FakeComplianceService {
  calls = {
    strip: [] as any[],
    legalHold: [] as any[],
    batch: [] as any[],
  };

  reset() {
    this.calls.strip.length = 0;
    this.calls.legalHold.length = 0;
    this.calls.batch.length = 0;
  }

  async stripPii(dto: any, actor: any) {
    this.calls.strip.push([dto, actor]);
    return { ok: true, dto, actor };
  }

  async setLegalHold(dto: any, actor: any) {
    this.calls.legalHold.push([dto, actor]);
    return { ok: true, dto, actor };
  }

  async batchArchive(dto: any, actor: any) {
    this.calls.batch.push([dto, actor]);
    return { ok: true, dto, actor };
  }

  async status() {
    return { status: 'ready' };
  }
}

class FakeBulkService {
  calls: any[] = [];

  reset() {
    this.calls.length = 0;
  }

  async import(entityType: any, options: any) {
    this.calls.push(['import', entityType, options]);
    return { entityType, processed: 1, created: 1, updated: 0 };
  }

  async export(entityType: any, options: any) {
    this.calls.push(['export', entityType, options]);
    return { entityType, format: 'json', count: 0, data: [] };
  }
}

describe('Compliance and Bulk controllers (HTTP)', () => {
  let app: INestApplication;
  const compliance = new FakeComplianceService();
  const bulk = new FakeBulkService();

  const withUser = (user: any) => ({ 'x-test-user': JSON.stringify(user) });

  before(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [ComplianceController, BulkController],
      providers: [
        SessionGuard,
        OrgScopeGuard,
        RolesGuard,
        Reflector,
        { provide: ComplianceService, useValue: compliance },
        { provide: BulkService, useValue: bulk },
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
          // ignore parse errors; guards will deny
        }
      }
      next();
    });
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
      })
    );
    await app.init();
  });

  beforeEach(() => {
    compliance.reset();
    bulk.reset();
  });

  after(async () => {
    await app.close();
  });

  it('validates strip-pii payload and forwards actor', async () => {
    const res = await request(app.getHttpServer())
      .post('/compliance/strip-pii')
      .set(withUser({ id: 'c1', orgId: 'org-1', role: Role.ComplianceOfficer, roles: [Role.ComplianceOfficer] }))
      .send({ entityType: 'user', entityId: 'user-123' })
      .expect(201);

    assert.equal(res.body.ok, true);
    const call = compliance.calls.strip[0];
    assert.ok(call);
    assert.equal(call[0].entityType, 'user');
    assert.equal(call[1].id, 'c1');
  });

  it('rejects invalid entity type on strip-pii', async () => {
    await request(app.getHttpServer())
      .post('/compliance/strip-pii')
      .set(withUser({ id: 'c2', orgId: 'org-1', role: Role.Admin, roles: [Role.Admin] }))
      .send({ entityType: 'unknown', entityId: 'id-1' })
      .expect(400);

    assert.equal(compliance.calls.strip.length, 0);
  });

  it('enforces org context for non-privileged actors on legal hold', async () => {
    await request(app.getHttpServer())
      .post('/compliance/legal-hold')
      .set(withUser({ id: 'admin-1', role: Role.Admin, roles: [Role.Admin] }))
      .send({ entityType: 'project', entityId: 'p-1', legalHold: true })
      .expect(403);

    assert.equal(compliance.calls.legalHold.length, 0);
  });

  it('requires boolean legalHold value', async () => {
    await request(app.getHttpServer())
      .post('/compliance/legal-hold')
      .set(withUser({ id: 'c3', orgId: 'org-1', role: Role.ComplianceOfficer, roles: [Role.ComplianceOfficer] }))
      .send({ entityType: 'project', entityId: 'p-1', legalHold: 'yes' })
      .expect(400);

    assert.equal(compliance.calls.legalHold.length, 0);
  });

  it('enforces org scope on bulk import for non-privileged roles', async () => {
    await request(app.getHttpServer())
      .post('/bulk-import/contacts')
      .set(withUser({ id: 'user-1', role: Role.Admin, roles: [Role.Admin] }))
      .send({ records: [{ email: 'a@example.com' }] })
      .expect(403);

    assert.equal(bulk.calls.length, 0);
  });

  it('allows platform admin to target another org on bulk import', async () => {
    const res = await request(app.getHttpServer())
      .post('/bulk-import/contacts?orgId=org-9')
      .set(withUser({ id: 'plat-1', role: Role.PlatformAdmin, roles: [Role.PlatformAdmin] }))
      .send({ records: [{ email: 'a@example.com' }] })
      .expect(201);

    assert.equal(res.body.processed, 1);
    const call = bulk.calls.find(([action]) => action === 'import');
    assert.ok(call);
    assert.equal(call[2].orgId, 'org-9');
    assert.equal(call[2].actor.role, Role.PlatformAdmin);
  });
});
