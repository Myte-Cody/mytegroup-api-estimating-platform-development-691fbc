import test from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';
import { Test } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import { IngestionContactsController } from '../src/features/ingestion/ingestion-contacts.controller';
import { IngestionContactsService } from '../src/features/ingestion/ingestion-contacts.service';
import { SessionGuard } from '../src/common/guards/session.guard';
import { OrgScopeGuard } from '../src/common/guards/org-scope.guard';
import { RolesGuard } from '../src/common/guards/roles.guard';
import { Role } from '../src/common/roles';
import { AiService } from '../src/common/services/ai.service';
import { AuditLogService } from '../src/common/services/audit-log.service';

test('POST /ingestion/contacts/v1/suggest-mapping works', async () => {
  const moduleRef = await Test.createTestingModule({
    controllers: [IngestionContactsController],
    providers: [
      IngestionContactsService,
      {
        provide: AiService,
        useValue: {
          isEnabled: () => false,
        },
      },
      {
        provide: AuditLogService,
        useValue: { log: async () => {} },
      },
    ],
  })
    .overrideGuard(SessionGuard)
    .useValue({
      canActivate(ctx: any) {
        const req = ctx.switchToHttp().getRequest();
        req.session = { user: { id: 'u1', orgId: 'o1', role: Role.Admin } };
        return true;
      },
    })
    .overrideGuard(OrgScopeGuard)
    .useValue({ canActivate: () => true })
    .overrideGuard(RolesGuard)
    .useValue({ canActivate: () => true })
    .compile();

  const app: INestApplication = moduleRef.createNestApplication();
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  await app.init();

  const res = await request(app.getHttpServer())
    .post('/ingestion/contacts/v1/suggest-mapping')
    .send({ headers: ['Full Name', 'Email'], profile: 'mixed' })
    .expect(201);

  assert.equal(res.body.orgId, 'o1');
  assert.equal(res.body.suggestions.displayName, 'Full Name');
  assert.equal(res.body.suggestions.emails, 'Email');

  await app.close();
});

test('POST /ingestion/contacts/v1/parse-row returns candidates', async () => {
  const moduleRef = await Test.createTestingModule({
    controllers: [IngestionContactsController],
    providers: [
      IngestionContactsService,
      {
        provide: AiService,
        useValue: {
          isEnabled: () => false,
        },
      },
      {
        provide: AuditLogService,
        useValue: { log: async () => {} },
      },
    ],
  })
    .overrideGuard(SessionGuard)
    .useValue({
      canActivate(ctx: any) {
        const req = ctx.switchToHttp().getRequest();
        req.session = { user: { id: 'u1', orgId: 'o1', role: Role.Admin } };
        return true;
      },
    })
    .overrideGuard(OrgScopeGuard)
    .useValue({ canActivate: () => true })
    .overrideGuard(RolesGuard)
    .useValue({ canActivate: () => true })
    .compile();

  const app: INestApplication = moduleRef.createNestApplication();
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  await app.init();

  const res = await request(app.getHttpServer())
    .post('/ingestion/contacts/v1/parse-row')
    .send({
      profile: 'mixed',
      cells: { contact: 'a@x.com; b@y.com', name: 'A; B' },
      allowAiProcessing: false,
    })
    .expect(201);

  assert.equal(res.body.orgId, 'o1');
  assert.equal(res.body.mode, 'deterministic');
  assert.equal(res.body.candidates.length, 2);

  await app.close();
});

test('POST /ingestion/contacts/v1/enrich returns suggestions', async () => {
  const moduleRef = await Test.createTestingModule({
    controllers: [IngestionContactsController],
    providers: [
      IngestionContactsService,
      {
        provide: AiService,
        useValue: {
          isEnabled: () => false,
        },
      },
      {
        provide: AuditLogService,
        useValue: { log: async () => {} },
      },
    ],
  })
    .overrideGuard(SessionGuard)
    .useValue({
      canActivate(ctx: any) {
        const req = ctx.switchToHttp().getRequest();
        req.session = { user: { id: 'u1', orgId: 'o1', role: Role.Admin } };
        return true;
      },
    })
    .overrideGuard(OrgScopeGuard)
    .useValue({ canActivate: () => true })
    .overrideGuard(RolesGuard)
    .useValue({ canActivate: () => true })
    .compile();

  const app: INestApplication = moduleRef.createNestApplication();
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  await app.init();

  const res = await request(app.getHttpServer())
    .post('/ingestion/contacts/v1/enrich')
    .send({
      profile: 'mixed',
      candidate: { person: { emails: ['john.doe@acme.com'] } },
      allowAiProcessing: false,
    })
    .expect(201);

  assert.equal(res.body.orgId, 'o1');
  assert.equal(res.body.mode, 'deterministic');
  assert.equal(res.body.suggestions.person.displayName, 'John Doe');
  assert.equal(res.body.suggestions.companyName, 'Acme');

  await app.close();
});
