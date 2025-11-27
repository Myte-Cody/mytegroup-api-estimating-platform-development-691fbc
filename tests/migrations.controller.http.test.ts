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
import { MigrationsController } from '../src/features/migrations/migrations.controller.ts';
import { MigrationsService } from '../src/features/migrations/migrations.service.ts';

class FakeMigrationsService {
  calls = {
    start: [] as any[],
    status: [] as any[],
    abort: [] as any[],
    finalize: [] as any[],
  };

  reset() {
    this.calls.start.length = 0;
    this.calls.status.length = 0;
    this.calls.abort.length = 0;
    this.calls.finalize.length = 0;
  }

  async start(dto: any, actor: any) {
    this.calls.start.push([dto, actor]);
    return { id: 'mig-1', orgId: dto.orgId, dryRun: dto.dryRun || false };
  }

  async status(orgId: string) {
    this.calls.status.push([orgId]);
    return { id: 'mig-1', orgId, status: 'ready_for_cutover' };
  }

  async abort(dto: any, actor: any) {
    this.calls.abort.push([dto, actor]);
    return { id: dto.migrationId, status: 'aborted' };
  }

  async finalize(dto: any, actor: any) {
    this.calls.finalize.push([dto, actor]);
    return { id: dto.migrationId, status: 'completed' };
  }
}

describe('MigrationsController (HTTP)', () => {
  let app: INestApplication;
  const service = new FakeMigrationsService();

  const withUser = (user: any) => ({ 'x-test-user': JSON.stringify(user) });

  before(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [MigrationsController],
      providers: [
        SessionGuard,
        OrgScopeGuard,
        RolesGuard,
        Reflector,
        { provide: MigrationsService, useValue: service },
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
          // ignore parse errors
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
    service.reset();
  });

  after(async () => {
    await app.close();
  });

  it('blocks platform admin from migration endpoints', async () => {
    await request(app.getHttpServer())
      .post('/migration/start')
      .set(withUser({ id: 'plat', role: Role.PlatformAdmin, roles: [Role.PlatformAdmin] }))
      .send({ orgId: 'org-1', direction: 'shared_to_dedicated' })
      .expect(403);

    assert.equal(service.calls.start.length, 0);
  });

  it('requires required fields and boolean types on start', async () => {
    await request(app.getHttpServer())
      .post('/migration/start')
      .set(withUser({ id: 'super', role: Role.SuperAdmin }))
      .send({ direction: 'shared_to_dedicated', dryRun: 'true' })
      .expect(400);

    assert.equal(service.calls.start.length, 0);
  });

  it('forwards payload for start and finalize for superadmin', async () => {
    const startRes = await request(app.getHttpServer())
      .post('/migration/start')
      .set(withUser({ id: 'super-1', role: Role.SuperAdmin }))
      .send({ orgId: 'org-9', direction: 'shared_to_dedicated', dryRun: false })
      .expect(201);

    assert.equal(startRes.body.orgId, 'org-9');
    const startCall = service.calls.start[0];
    assert.ok(startCall);
    assert.equal(startCall[0].orgId, 'org-9');
    assert.equal(startCall[1].role, Role.SuperAdmin);

    const finalizeRes = await request(app.getHttpServer())
      .post('/migration/finalize')
      .set(withUser({ id: 'super-1', role: Role.SuperAdmin }))
      .send({ migrationId: 'mig-1', orgId: 'org-9', confirmCutover: true })
      .expect(201);

    assert.equal(finalizeRes.body.status, 'completed');
    const finalizeCall = service.calls.finalize[0];
    assert.ok(finalizeCall);
    assert.equal(finalizeCall[0].confirmCutover, true);
  });

  it('enforces boolean confirmCutover on finalize', async () => {
    await request(app.getHttpServer())
      .post('/migration/finalize')
      .set(withUser({ id: 'super-2', role: Role.SuperAdmin }))
      .send({ migrationId: 'mig-1', orgId: 'org-9', confirmCutover: 'yes' })
      .expect(400);

    assert.equal(service.calls.finalize.length, 0);
  });
});
