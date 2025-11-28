import { strict as assert } from 'node:assert';
import { after, before, describe, it } from 'node:test';
import request from 'supertest';
import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { Reflector } from '@nestjs/core';
import { Readable } from 'stream';
import { BulkController } from '../src/features/bulk/bulk.controller.ts';
import { BulkService } from '../src/features/bulk/bulk.service.ts';
import { SessionGuard } from '../src/common/guards/session.guard.ts';
import { OrgScopeGuard } from '../src/common/guards/org-scope.guard.ts';
import { RolesGuard } from '../src/common/guards/roles.guard.ts';
import { Role } from '../src/common/roles.ts';

describe('BulkController HTTP', () => {
  let app: INestApplication;
  const calls: any[] = [];
  const service: any = {
    importImpl: async () => ({
      entityType: 'contacts',
      dryRun: true,
      processed: 1,
      created: 1,
      updated: 0,
      errors: [],
    }),
    exportImpl: async () => ({
      entityType: 'contacts',
      format: 'json',
      count: 0,
      data: [],
    }),
    import: (...args: any[]) => service.importImpl(...args),
    export: (...args: any[]) => service.exportImpl(...args),
  };

  const withUser = (user: any) => ({ 'x-test-user': JSON.stringify(user) });

  before(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [BulkController],
      providers: [
        { provide: BulkService, useValue: service },
        SessionGuard,
        OrgScopeGuard,
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
          // ignore parse errors; guards will deny access
        }
      }
      next();
    });
    await app.init();
  });

  after(async () => {
    await app.close();
  });

  it('imports CSV via HTTP and forwards actor, dryRun, filename, and format hint', async () => {
    calls.length = 0;
    service.importImpl = async (...args: any[]) => {
      calls.push(['import', ...args]);
      return {
        entityType: 'contacts',
        dryRun: true,
        processed: 2,
        created: 2,
        updated: 0,
        errors: [],
      };
    };

    const csv = 'name,email,roles\nAlice,alice@example.com,manager;viewer\nBob,bob@example.com,pm\n';
    const res = await request(app.getHttpServer())
      .post('/bulk-import/contacts?dryRun=true')
      .set(withUser({ id: 'admin-1', orgId: 'org-1', role: Role.Admin, roles: [Role.Admin] }))
      .field('format', 'csv')
      .attach('file', Buffer.from(csv, 'utf-8'), 'contacts.csv')
      .expect(201);

    assert.equal(res.body.processed, 2);
    const call = calls.find(([action]) => action === 'import');
    assert.ok(call, 'expected import to be called');
    const [, entityType, options] = call;
    assert.equal(entityType, 'contacts');
    assert.equal(options.dryRun, true);
    assert.equal(options.actor.id, 'admin-1');
    assert.equal(options.file?.originalname, 'contacts.csv');
    assert.equal(options.formatHint, 'csv');
  });

  it('exports CSV via HTTP with includeArchived flag and streams response', async () => {
    calls.length = 0;
    service.exportImpl = async (...args: any[]) => {
      calls.push(['export', ...args]);
      return {
        entityType: 'contacts',
        format: 'csv',
        count: 1,
        filename: 'contacts-export.csv',
        stream: Readable.from(['name,email\nAlice,alice@example.com\n']),
      };
    };

    const res = await request(app.getHttpServer())
      .get('/bulk-export/contacts?format=csv&includeArchived=true')
      .set(withUser({ id: 'owner-1', orgId: 'org-1', role: Role.OrgOwner, roles: [Role.OrgOwner] }))
      .expect(200);

    assert.match(res.text, /Alice/);
    assert.equal(res.headers['content-type'], 'text/csv');
    assert.match(res.headers['content-disposition'], /contacts-export/);
    const call = calls.find(([action]) => action === 'export');
    assert.ok(call);
    const [, entityType, options] = call;
    assert.equal(entityType, 'contacts');
    assert.equal(options.includeArchived, true);
    assert.equal(options.format, 'csv');
  });

  it('allows superadmin to override org on JSON export', async () => {
    calls.length = 0;
    service.exportImpl = async (...args: any[]) => {
      calls.push(['export', ...args]);
      return {
        entityType: 'users',
        format: 'json',
        count: 1,
        data: [{ email: 'user@example.com', username: 'User' }],
      };
    };

    const res = await request(app.getHttpServer())
      .get('/bulk-export/users?format=json&orgId=target-org')
      .set(withUser({ id: 'super-1', role: Role.SuperAdmin }))
      .expect(200);

    assert.equal(res.body.count, 1);
    const call = calls.find(([action]) => action === 'export');
    assert.ok(call);
    const [, entityType, options] = call;
    assert.equal(entityType, 'users');
    assert.equal(options.orgId, 'target-org');
    assert.equal(options.format, 'json');
    assert.equal(options.includeArchived, false);
  });
});
