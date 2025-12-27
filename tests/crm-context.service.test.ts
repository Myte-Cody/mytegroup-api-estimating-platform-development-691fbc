import { strict as assert } from 'node:assert';
import { afterEach, beforeEach, describe, it } from 'node:test';
import { MongoMemoryServer } from 'mongodb-memory-server';
import { Connection, createConnection } from 'mongoose';

import { Role } from '../src/common/roles';
import { TenantConnectionService } from '../src/common/tenancy/tenant-connection.service';
import { normalizeName } from '../src/common/utils/normalize.util';
import { CrmContextService } from '../src/features/crm-context/crm-context.service';
import { CompanyLocationSchema } from '../src/features/company-locations/schemas/company-location.schema';
import { CompanySchema } from '../src/features/companies/schemas/company.schema';
import { GraphEdgeSchema } from '../src/features/graph-edges/schemas/graph-edge.schema';
import { OfficeSchema } from '../src/features/offices/schemas/office.schema';
import { OrganizationSchema } from '../src/features/organizations/schemas/organization.schema';
import { PersonSchema } from '../src/features/persons/schemas/person.schema';

describe('CrmContextService', () => {
  let server: MongoMemoryServer;
  let connection: Connection;
  let tenants: TenantConnectionService;
  let svc: CrmContextService;

  beforeEach(async () => {
    server = await MongoMemoryServer.create();
    connection = await createConnection(server.getUri(), { dbName: 'sharedDb' }).asPromise();

    const orgModel = connection.model('Organization', OrganizationSchema);
    const companyModel = connection.model('Company', CompanySchema);
    const locationModel = connection.model('CompanyLocation', CompanyLocationSchema);
    const personModel = connection.model('Person', PersonSchema);
    const officeModel = connection.model('Office', OfficeSchema);
    const edgeModel = connection.model('GraphEdge', GraphEdgeSchema);

    tenants = new TenantConnectionService(connection, orgModel as any);
    svc = new CrmContextService(
      orgModel as any,
      companyModel as any,
      locationModel as any,
      personModel as any,
      officeModel as any,
      edgeModel as any,
      tenants
    );
  });

  afterEach(async () => {
    if (connection) await connection.close();
    if (server) await server.stop();
  });

  it('exports companies with archived filtering and pagination', async () => {
    const orgModel = connection.model('Organization');
    const org = await orgModel.create({ name: 'Org A', useDedicatedDb: false });
    const companyModel = await tenants.getModelForOrg(org.id, 'Company', CompanySchema, connection.model('Company'));

    await companyModel.create({ orgId: org.id, name: 'Acme Steel', normalizedName: normalizeName('Acme Steel') });
    await companyModel.create({
      orgId: org.id,
      name: 'Archived Co',
      normalizedName: normalizeName('Archived Co'),
      archivedAt: new Date(),
    });

    const actor = { userId: 'user-1', orgId: org.id, role: Role.OrgAdmin };

    const activeOnly = await svc.listDocuments(actor, org.id, {
      entityType: 'company',
      page: 1,
      limit: 25,
    } as any);

    assert.equal(activeOnly.total, 1);
    assert.equal(activeOnly.data.length, 1);
    assert.equal(activeOnly.data[0].entityType, 'company');
    assert.ok(activeOnly.data[0].docId.startsWith(`crm:company:${org.id}:`));
    assert.match(activeOnly.data[0].text, /Company: Acme Steel/);

    const withArchived = await svc.listDocuments(actor, org.id, {
      entityType: 'company',
      includeArchived: true,
      page: 1,
      limit: 25,
    } as any);

    assert.equal(withArchived.total, 2);
    assert.equal(withArchived.data.length, 2);
  });

  it('exports graph edges', async () => {
    const orgModel = connection.model('Organization');
    const org = await orgModel.create({ name: 'Org B', useDedicatedDb: false });
    const edgeModel = await tenants.getModelForOrg(org.id, 'GraphEdge', GraphEdgeSchema, connection.model('GraphEdge'));

    const edge = await edgeModel.create({
      orgId: org.id,
      edgeTypeKey: 'reports_to',
      fromNodeType: 'person',
      fromNodeId: 'person-a',
      toNodeType: 'person',
      toNodeId: 'person-b',
      metadata: { relationshipStrength: 'dotted' },
    });

    const actor = { userId: 'user-2', orgId: org.id, role: Role.OrgAdmin };
    const res = await svc.listDocuments(actor, org.id, {
      entityType: 'graph_edge',
      page: 1,
      limit: 25,
    } as any);

    assert.equal(res.total, 1);
    assert.equal(res.data[0].entityId, edge.id);
    assert.equal(res.data[0].entityType, 'graph_edge');
    assert.match(res.data[0].text, /Graph edge: reports_to/);
  });

  it('exports org locations with address field', async () => {
    const orgModel = connection.model('Organization');
    const org = await orgModel.create({ name: 'Org C', useDedicatedDb: false });
    const officeModel = await tenants.getModelForOrg(org.id, 'Office', OfficeSchema, connection.model('Office'));

    await officeModel.create({
      orgId: org.id,
      name: 'HQ',
      normalizedName: normalizeName('HQ'),
      address: '123 Main St',
    });

    const actor = { userId: 'user-3', orgId: org.id, role: Role.OrgAdmin };
    const res = await svc.listDocuments(actor, org.id, {
      entityType: 'org_location',
      page: 1,
      limit: 25,
    } as any);

    assert.equal(res.total, 1);
    assert.match(res.data[0].text, /Org location: HQ/);
    assert.match(res.data[0].text, /Address: 123 Main St/);
  });
});
