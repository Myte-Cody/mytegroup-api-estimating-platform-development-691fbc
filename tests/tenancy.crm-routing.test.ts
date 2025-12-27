const { describe, it, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const { MongoMemoryServer } = require('mongodb-memory-server');
const { createConnection } = require('mongoose');

const { TenantConnectionService } = require('../src/common/tenancy/tenant-connection.service');
const { normalizeName } = require('../src/common/utils/normalize.util');

const { OrganizationSchema } = require('../src/features/organizations/schemas/organization.schema');
const { PersonSchema } = require('../src/features/persons/schemas/person.schema');
const { CompanySchema } = require('../src/features/companies/schemas/company.schema');
const { OfficeSchema } = require('../src/features/offices/schemas/office.schema');

describe('TenantConnectionService CRM routing', () => {
  let server;
  let sharedConn;
  let orgModel;
  let tenants;
  let dedicatedOrgId;

  beforeEach(async () => {
    server = await MongoMemoryServer.create();
    sharedConn = await createConnection(server.getUri(), { dbName: 'sharedDb' }).asPromise();

    orgModel = sharedConn.model('Organization', OrganizationSchema);
    sharedConn.model('Person', PersonSchema);
    sharedConn.model('Company', CompanySchema);
    sharedConn.model('Office', OfficeSchema);

    tenants = new TenantConnectionService(sharedConn, orgModel);
    dedicatedOrgId = undefined;
  });

  afterEach(async () => {
    if (dedicatedOrgId) {
      await tenants.resetConnectionForOrg(dedicatedOrgId);
    }
    if (sharedConn) await sharedConn.close();
    if (server) await server.stop();
  });

  it('routes Person/Company/OrgLocation models to the correct database', async () => {
    const sharedOrg = await orgModel.create({ name: 'Shared Org', useDedicatedDb: false });
    const dedicatedOrg = await orgModel.create({
      name: 'Dedicated Org',
      useDedicatedDb: true,
      databaseUri: server.getUri(),
      databaseName: 'dedicatedDb',
      dataResidency: 'dedicated',
    });
    dedicatedOrgId = dedicatedOrg.id;

    const defaultPersonModel = sharedConn.model('Person');
    const defaultCompanyModel = sharedConn.model('Company');
    const defaultOfficeModel = sharedConn.model('Office');

    const sharedPerson = await tenants.getModelForOrg(sharedOrg.id, 'Person', PersonSchema, defaultPersonModel);
    const dedicatedPerson = await tenants.getModelForOrg(dedicatedOrg.id, 'Person', PersonSchema, defaultPersonModel);

    await sharedPerson.create({ orgId: sharedOrg.id, personType: 'internal_staff', displayName: 'Alice Shared' });
    await dedicatedPerson.create({ orgId: dedicatedOrg.id, personType: 'internal_staff', displayName: 'Bob Dedicated' });

    assert.strictEqual(await defaultPersonModel.countDocuments({ orgId: sharedOrg.id }), 1);
    assert.strictEqual(await defaultPersonModel.countDocuments({ orgId: dedicatedOrg.id }), 0);
    assert.strictEqual(await dedicatedPerson.countDocuments({ orgId: dedicatedOrg.id }), 1);
    assert.strictEqual(await dedicatedPerson.countDocuments({ orgId: sharedOrg.id }), 0);

    const sharedCompany = await tenants.getModelForOrg(sharedOrg.id, 'Company', CompanySchema, defaultCompanyModel);
    const dedicatedCompany = await tenants.getModelForOrg(dedicatedOrg.id, 'Company', CompanySchema, defaultCompanyModel);

    await sharedCompany.create({
      orgId: sharedOrg.id,
      name: 'Acme Steel',
      normalizedName: normalizeName('Acme Steel'),
    });
    await dedicatedCompany.create({
      orgId: dedicatedOrg.id,
      name: 'Acme Steel',
      normalizedName: normalizeName('Acme Steel'),
    });

    assert.strictEqual(await defaultCompanyModel.countDocuments({ orgId: sharedOrg.id }), 1);
    assert.strictEqual(await defaultCompanyModel.countDocuments({ orgId: dedicatedOrg.id }), 0);
    assert.strictEqual(await dedicatedCompany.countDocuments({ orgId: dedicatedOrg.id }), 1);
    assert.strictEqual(await dedicatedCompany.countDocuments({ orgId: sharedOrg.id }), 0);

    const sharedOffice = await tenants.getModelForOrg(sharedOrg.id, 'Office', OfficeSchema, defaultOfficeModel);
    const dedicatedOffice = await tenants.getModelForOrg(dedicatedOrg.id, 'Office', OfficeSchema, defaultOfficeModel);

    await sharedOffice.create({ orgId: sharedOrg.id, name: 'HQ' });
    await dedicatedOffice.create({ orgId: dedicatedOrg.id, name: 'HQ' });

    assert.strictEqual(await defaultOfficeModel.countDocuments({ orgId: sharedOrg.id }), 1);
    assert.strictEqual(await defaultOfficeModel.countDocuments({ orgId: dedicatedOrg.id }), 0);
    assert.strictEqual(await dedicatedOffice.countDocuments({ orgId: dedicatedOrg.id }), 1);
    assert.strictEqual(await dedicatedOffice.countDocuments({ orgId: sharedOrg.id }), 0);

    const dedicatedConn = await tenants.getConnectionForOrg(dedicatedOrg.id);
    assert.notStrictEqual(dedicatedConn, sharedConn);
    assert.strictEqual(dedicatedConn.db.databaseName, 'dedicatedDb');
  });
});

