import { after, before, beforeEach, describe, it } from 'node:test';
import * as assert from 'node:assert/strict';
import mongoose, { Connection, Model } from 'mongoose';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { ForbiddenException, NotFoundException } from '@nestjs/common';
import { Role } from '../src/common/roles';
import { EventLog, EventLogSchema } from '../src/common/events/event-log.schema';
import { EventLogService } from '../src/common/events/event-log.service';
import { AuditLogService } from '../src/common/services/audit-log.service';
import { Organization, OrganizationSchema } from '../src/features/organizations/schemas/organization.schema';
import { Project, ProjectSchema } from '../src/features/projects/schemas/project.schema';
import { Estimate, EstimateSchema } from '../src/features/estimates/schemas/estimate.schema';
import { EstimatesService } from '../src/features/estimates/estimates.service';

describe('EstimatesService', { concurrency: 1 }, () => {
  let mongod: MongoMemoryServer;
  let connection: Connection;
  let orgModel: Model<Organization>;
  let projectModel: Model<Project>;
  let estimateModel: Model<Estimate>;
  let eventLogModel: Model<EventLog>;
  let service: EstimatesService;

  let orgAId: string;
  let orgBId: string;
  let projectAId: string;

  const estimatorActor = () => ({ userId: 'est-1', orgId: orgAId, role: Role.Estimator });
  const adminActor = () => ({ userId: 'admin-1', orgId: orgAId, role: Role.Admin });
  const viewerActor = () => ({ userId: 'viewer-1', orgId: orgAId, role: Role.Viewer });
  const otherOrgEstimator = () => ({ userId: 'est-2', orgId: orgBId, role: Role.Estimator });

  before(async () => {
    mongod = await MongoMemoryServer.create();
    connection = await mongoose.createConnection(mongod.getUri(), { dbName: 'testdb' }).asPromise();
    orgModel = connection.model<Organization>('Organization', OrganizationSchema);
    projectModel = connection.model<Project>('Project', ProjectSchema);
    estimateModel = connection.model<Estimate>('Estimate', EstimateSchema);
    eventLogModel = connection.model<EventLog>('EventLog', EventLogSchema);
    const eventLog = new EventLogService(eventLogModel);
    const audit = new AuditLogService(eventLog);
    service = new EstimatesService(estimateModel, projectModel, orgModel, audit);
  });

  beforeEach(async () => {
    await Promise.all([
      orgModel.deleteMany({}),
      projectModel.deleteMany({}),
      estimateModel.deleteMany({}),
      eventLogModel.deleteMany({}),
    ]);

    const [orgA, orgB] = await Promise.all([
      orgModel.create({ name: 'Org A', useDedicatedDb: false }),
      orgModel.create({ name: 'Org B', useDedicatedDb: false }),
    ]);
    orgAId = orgA.id;
    orgBId = orgB.id;
    const projectA = await projectModel.create({ name: 'Project A', organizationId: orgAId });
    projectAId = projectA.id;
  });

  after(async () => {
    await connection?.close();
    await mongod?.stop();
  });

  it('creates and lists estimates with audit', async () => {
    const created = await service.create(projectAId, { name: 'Base Estimate' }, estimatorActor());
    const estimateId = (created as any).id || (created as any)._id;
    assert.ok(estimateId);
    assert.equal((created as any).projectId, projectAId);
    assert.equal((created as any).organizationId, orgAId);
    assert.equal(await eventLogModel.countDocuments({ eventType: 'estimate.created' }), 1);

    const list = await service.list(projectAId, estimatorActor(), false);
    assert.equal(list.length, 1);
  });

  it('enforces project org scoping', async () => {
    await assert.rejects(service.create(projectAId, { name: 'CrossOrg' }, otherOrgEstimator()), ForbiddenException);
  });

  it('blocks includeArchived for non-admin roles', async () => {
    const created = await service.create(projectAId, { name: 'ArchivedCheck' }, estimatorActor());
    const estimateId = (created as any).id || (created as any)._id;
    await service.archive(projectAId, estimateId, adminActor());
    await assert.rejects(service.list(projectAId, estimatorActor(), true), ForbiddenException);
  });

  it('archives, unarchives, and respects includeArchived', async () => {
    const created = await service.create(projectAId, { name: 'ToArchive' }, estimatorActor());
    const estimateId = (created as any).id || (created as any)._id;
    await service.archive(projectAId, estimateId, adminActor());

    const listDefault = await service.list(projectAId, adminActor(), false);
    assert.equal(listDefault.length, 0);
    const listWithArchived = await service.list(projectAId, adminActor(), true);
    assert.equal(listWithArchived.length, 1);

    const archived = await service.getById(projectAId, estimateId, adminActor(), true);
    assert.ok((archived as any).archivedAt);
    assert.equal((archived as any).status, 'archived');

    await service.unarchive(projectAId, estimateId, adminActor());
    const unarchived = await service.getById(projectAId, estimateId, adminActor(), false);
    assert.equal((unarchived as any).archivedAt, null);
  });

  it('updates line items, bumps revision, and computes totals', async () => {
    const created = await service.create(projectAId, { name: 'LineItems' }, adminActor());
    const estimateId = (created as any).id || (created as any)._id;

    const updated = await service.update(
      projectAId,
      estimateId,
      {
        lineItems: [
          { code: '01', description: 'Concrete', quantity: 10, unit: 'CY', unitCost: 100 },
          { code: '02', description: 'Steel', quantity: 5, unit: 'TN', unitCost: 200 },
        ],
      },
      adminActor()
    );

    assert.equal((updated as any).revision, 2);
    assert.equal((updated as any).totalAmount, 10 * 100 + 5 * 200);
    assert.equal(await eventLogModel.countDocuments({ eventType: 'estimate.updated' }), 1);
  });

  it('blocks updates when estimate is on legal hold', async () => {
    const created = await service.create(projectAId, { name: 'OnHold' }, adminActor());
    const estimateId = (created as any).id || (created as any)._id;
    await estimateModel.updateOne({ _id: estimateId }, { $set: { legalHold: true } });
    await assert.rejects(
      service.update(projectAId, estimateId, { description: 'Nope' }, adminActor()),
      ForbiddenException
    );
  });

  it('blocks writes when org or project legal hold is active', async () => {
    await orgModel.updateOne({ _id: orgAId }, { $set: { legalHold: true } });
    await assert.rejects(service.create(projectAId, { name: 'BlockedOrgHold' }, adminActor()), ForbiddenException);

    await orgModel.updateOne({ _id: orgAId }, { $set: { legalHold: false } });
    await projectModel.updateOne({ _id: projectAId }, { $set: { legalHold: true } });
    await assert.rejects(service.create(projectAId, { name: 'BlockedProjectHold' }, adminActor()), ForbiddenException);
  });

  it('returns not found for archived estimate unless includeArchived', async () => {
    const created = await service.create(projectAId, { name: 'Archived' }, adminActor());
    const estimateId = (created as any).id || (created as any)._id;
    await service.archive(projectAId, estimateId, adminActor());

    await assert.rejects(service.getById(projectAId, estimateId, adminActor(), false), NotFoundException);
    const archived = await service.getById(projectAId, estimateId, adminActor(), true);
    assert.ok((archived as any).archivedAt);
  });

  it('denies viewers', async () => {
    await assert.rejects(service.create(projectAId, { name: 'Nope' }, viewerActor()), ForbiddenException);
  });
});

