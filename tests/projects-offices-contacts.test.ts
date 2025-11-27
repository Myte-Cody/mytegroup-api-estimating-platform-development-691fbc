import { describe, it, before, after, beforeEach } from 'node:test';
import * as assert from 'node:assert/strict';
import mongoose, { Connection, Model } from 'mongoose';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { ForbiddenException, NotFoundException, ConflictException } from '@nestjs/common';
import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { ProjectsService } from '../src/features/projects/projects.service';
import { Project, ProjectSchema } from '../src/features/projects/schemas/project.schema';
import { OfficesService } from '../src/features/offices/offices.service';
import { Office, OfficeSchema } from '../src/features/offices/schemas/office.schema';
import { ContactsService } from '../src/features/contacts/contacts.service';
import { Contact, ContactSchema } from '../src/features/contacts/schemas/contact.schema';
import { Organization, OrganizationSchema } from '../src/features/organizations/schemas/organization.schema';
import { EventLog, EventLogSchema } from '../src/common/events/event-log.schema';
import { EventLogService } from '../src/common/events/event-log.service';
import { AuditLogService } from '../src/common/services/audit-log.service';
import { TenantConnectionService } from '../src/common/tenancy/tenant-connection.service';
import { Role } from '../src/common/roles';
import { CreateContactDto } from '../src/features/contacts/dto/create-contact.dto';

type OrgModel = Model<Organization>;

describe('Projects/Offices/Contacts services', { concurrency: 1 }, () => {
  let mongod: MongoMemoryServer;
  let connection: Connection;
  let projectModel: Model<Project>;
  let officeModel: Model<Office>;
  let contactModel: Model<Contact>;
  let orgModel: OrgModel;
  let eventLogModel: Model<EventLog>;
  let projects: ProjectsService;
  let offices: OfficesService;
  let contacts: ContactsService;
  let orgAId: string;
  let orgBId: string;

  before(async () => {
    mongod = await MongoMemoryServer.create();
    connection = await mongoose.createConnection(mongod.getUri(), { dbName: 'testdb' }).asPromise();
    projectModel = connection.model<Project>('Project', ProjectSchema);
    officeModel = connection.model<Office>('Office', OfficeSchema);
    contactModel = connection.model<Contact>('Contact', ContactSchema);
    orgModel = connection.model<Organization>('Organization', OrganizationSchema);
    eventLogModel = connection.model<EventLog>('EventLog', EventLogSchema);
    const eventLog = new EventLogService(eventLogModel);
    const audit = new AuditLogService(eventLog);
    const tenant = new TenantConnectionService(connection, orgModel);
    projects = new ProjectsService(projectModel, orgModel, officeModel, audit);
    offices = new OfficesService(officeModel, orgModel, audit);
    contacts = new ContactsService(contactModel, audit, tenant);
  });

  beforeEach(async () => {
    await Promise.all([
      projectModel.deleteMany({}),
      officeModel.deleteMany({}),
      contactModel.deleteMany({}),
      orgModel.deleteMany({}),
      eventLogModel.deleteMany({}),
    ]);
    const orgA = await orgModel.create({ name: 'Org A', useDedicatedDb: false });
    const orgB = await orgModel.create({ name: 'Org B', useDedicatedDb: false });
    orgAId = orgA.id;
    orgBId = orgB.id;
  });

  after(async () => {
    await connection?.close();
    await mongod?.stop();
  });

  const adminActor = () => ({ userId: 'admin-user', orgId: orgAId, role: Role.Admin });
  const pmActor = () => ({ userId: 'pm-user', orgId: orgAId, role: Role.PM });
  const managerActor = () => ({ userId: 'manager-user', orgId: orgAId, role: Role.Manager });
  const otherOrgAdmin = () => ({ userId: 'other-admin', orgId: orgBId, role: Role.Admin });

  describe('ProjectsService', () => {
    it('creates, lists, archives with RBAC and audit', async () => {
      const office = await offices.create({ name: 'HQ', organizationId: orgAId, address: '123 Main' }, adminActor());
      const project = await projects.create(
        { name: 'Skyline', description: 'Tower build', organizationId: orgAId, officeId: office.id },
        adminActor()
      );
      const projectId = (project as any)._id || (project as any).id;
      assert.equal(project.organizationId, orgAId);
      assert.equal(await eventLogModel.countDocuments({ eventType: 'project.created' }), 1);

      await assert.rejects(
        projects.create({ name: 'Skyline', organizationId: orgAId }, adminActor()),
        ConflictException
      );

      await projects.archive(projectId, adminActor());
      const listDefault = await projects.list(adminActor(), orgAId);
      assert.equal(listDefault.length, 0);
      const listWithArchived = await projects.list(managerActor(), orgAId, true);
      assert.equal(listWithArchived.length, 1);
      await assert.rejects(projects.list(pmActor(), orgAId, true), ForbiddenException);
    });

    it('enforces org scope and update semantics', async () => {
      const office = await offices.create({ name: 'North', organizationId: orgAId }, adminActor());
      const project = await projects.create(
        { name: 'Bridge', organizationId: orgAId, officeId: office.id },
        adminActor()
      );
      const projectId = (project as any)._id || (project as any).id;
      await assert.rejects(projects.archive(projectId, otherOrgAdmin()), ForbiddenException);
      const updated = await projects.update(projectId, { description: 'updated scope' }, adminActor());
      assert.equal(updated.description, 'updated scope');
      await projects.archive(projectId, adminActor());
      await assert.rejects(projects.getById(projectId, adminActor(), false), NotFoundException);
      const archivedView = await projects.getById(projectId, adminActor(), true);
      assert.ok(archivedView.archivedAt);
      assert.equal(await eventLogModel.countDocuments({ eventType: 'project.updated' }), 1);
    });

    it('blocks archive/unarchive when legal hold is active', async () => {
      const office = await offices.create({ name: 'LegalHold', organizationId: orgAId }, adminActor());
      const project = await projects.create(
        { name: 'HoldProject', organizationId: orgAId, officeId: office.id },
        adminActor()
      );
      const projectId = (project as any)._id || (project as any).id;
      await projectModel.updateOne({ _id: projectId }, { $set: { legalHold: true } });
      await assert.rejects(projects.archive(projectId, adminActor()), ForbiddenException);
      await projectModel.updateOne({ _id: projectId }, { $set: { archivedAt: new Date(), legalHold: true } });
      await assert.rejects(projects.unarchive(projectId, adminActor()), ForbiddenException);
    });
  });

  describe('OfficesService', () => {
    it('supports CRUD + archive with scoped access', async () => {
      const office = await offices.create({ name: 'HQ', organizationId: orgAId, address: '123 Main' }, adminActor());
      const officeId = (office as any)._id || (office as any).id;
      await assert.rejects(offices.update(officeId, { address: 'New' }, otherOrgAdmin()), ForbiddenException);
      const updated = await offices.update(officeId, { address: 'New Address' }, managerActor());
      assert.equal(updated.address, 'New Address');
      await offices.archive(officeId, adminActor());
      await assert.rejects(offices.getById(officeId, adminActor(), false), NotFoundException);
      const archived = await offices.getById(officeId, adminActor(), true);
      assert.ok(archived.archivedAt);
      await assert.rejects(offices.list(pmActor(), orgAId, true), ForbiddenException);
    });

    it('blocks archive/unarchive when legal hold is active', async () => {
      const office = await offices.create({ name: 'LegalHold', organizationId: orgAId }, adminActor());
      const officeId = (office as any)._id || (office as any).id;
      await officeModel.updateOne({ _id: officeId }, { $set: { legalHold: true } });
      await assert.rejects(offices.archive(officeId, adminActor()), ForbiddenException);
      await officeModel.updateOne({ _id: officeId }, { $set: { archivedAt: new Date(), legalHold: true } });
      await assert.rejects(offices.unarchive(officeId, adminActor()), ForbiddenException);
    });
  });

  describe('ContactsService', () => {
    it('creates, archives, and filters by org with audit coverage', async () => {
      const contact = await contacts.create(adminActor(), orgAId, {
        name: 'Jane Doe',
        email: 'jane@example.com',
        phone: '+1234567890',
      });
      const contactId = (contact as any)._id || (contact as any).id;
      assert.equal(contact.orgId, orgAId);
      assert.equal(await eventLogModel.countDocuments({ eventType: 'contact.created' }), 1);

      await assert.rejects(
        contacts.create(adminActor(), orgAId, { name: 'Dup', email: 'jane@example.com', phone: '+1234567890' }),
        ConflictException
      );

      await contacts.archive(adminActor(), orgAId, contactId);
      await assert.rejects(contacts.update(adminActor(), orgAId, contactId, { name: 'New' }), NotFoundException);
      const defaultList = await contacts.list(adminActor(), orgAId);
      assert.equal(defaultList.length, 0);
      const withArchived = await contacts.list(managerActor(), orgAId, true);
      assert.equal(withArchived.length, 1);
      await assert.rejects(contacts.list(pmActor(), orgAId, true), ForbiddenException);
    });

    it('blocks cross-org access and handles archived reads', async () => {
      const contact = await contacts.create(adminActor(), orgAId, {
        name: 'Alex Stone',
        email: 'alex@example.com',
        phone: '+1234567899',
      });
      const contactId = (contact as any)._id || (contact as any).id;
      await assert.rejects(contacts.archive(otherOrgAdmin(), orgAId, contactId), ForbiddenException);
      await contacts.archive(adminActor(), orgAId, contactId);
      await assert.rejects(contacts.getById(adminActor(), contactId, orgAId, false), NotFoundException);
      const archived = await contacts.getById(managerActor(), contactId, orgAId, true);
      assert.ok(archived.archivedAt);
      await assert.rejects(contacts.list(adminActor(), orgBId), ForbiddenException);
    });

    it('validates DTO format for phone/email', async () => {
      const dto = plainToInstance(CreateContactDto, { name: '', phone: 'invalid' });
      const errors = await validate(dto);
      assert.ok(errors.length > 0);
    });

    it('blocks archive/unarchive when legal hold is active', async () => {
      const contact = await contacts.create(adminActor(), orgAId, {
        name: 'On Hold',
        email: 'hold@example.com',
        phone: '+1234567891',
      });
      const contactId = (contact as any)._id || (contact as any).id;
      await contactModel.updateOne({ _id: contactId }, { $set: { legalHold: true } });
      await assert.rejects(contacts.archive(adminActor(), orgAId, contactId), ForbiddenException);
      await contactModel.updateOne({ _id: contactId }, { $set: { archivedAt: new Date(), legalHold: true } });
      await assert.rejects(contacts.unarchive(adminActor(), orgAId, contactId), ForbiddenException);
    });
  });
});
