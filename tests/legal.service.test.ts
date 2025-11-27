require('reflect-metadata');
const { after, before, beforeEach, describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { MongoMemoryServer } = require('mongodb-memory-server');
const mongoose = require('mongoose');
const { LegalService } = require('../src/features/legal/legal.service.ts');
const { LegalDocSchema } = require('../src/features/legal/schemas/legal-doc.schema.ts');
const { LegalAcceptanceSchema } = require('../src/features/legal/schemas/legal-acceptance.schema.ts');
const { LegalDocType } = require('../src/features/legal/legal.types.ts');

describe('LegalService', () => {
  let mongo;
  let connection;
  let docModel;
  let acceptanceModel;
  let auditLog;
  let service;

  before(async () => {
    mongo = await MongoMemoryServer.create();
    connection = await mongoose.createConnection(mongo.getUri()).asPromise();
    docModel = connection.model('LegalDoc', LegalDocSchema);
    acceptanceModel = connection.model('LegalAcceptance', LegalAcceptanceSchema);
  });

  after(async () => {
    await connection?.close();
    await mongo?.stop();
  });

  beforeEach(async () => {
    auditLog = { calls: [], log: async function (evt) { this.calls.push(evt); } };
    service = new LegalService(docModel, acceptanceModel, auditLog);
    await docModel.deleteMany({});
    await acceptanceModel.deleteMany({});
  });

  it('creates a versioned legal doc and logs audit', async () => {
    const created = await service.createDoc(
      {
        type: LegalDocType.PrivacyPolicy,
        version: 'v1',
        content: 'Sample privacy policy text',
      },
      { id: 'admin-1' }
    );

    assert.equal(created.version, 'v1');
    assert.equal(created.type, LegalDocType.PrivacyPolicy);
    assert.equal(auditLog.calls.length, 1);
    assert.equal(auditLog.calls[0].eventType, 'legal.updated');
  });

  it('returns required acceptance when user has not accepted latest', async () => {
    await service.createDoc(
      { type: LegalDocType.Terms, version: '2024-01', content: 'Terms content' },
      { id: 'admin-2' }
    );

    const status = await service.acceptanceStatus({ id: 'user-1', orgId: 'org-1' });

    assert.equal(status.required.length, 1);
    assert.equal(status.required[0].type, LegalDocType.Terms);
    assert.equal(status.required[0].version, '2024-01');
  });

  it('accepts latest doc, is idempotent, and logs audit', async () => {
    await service.createDoc(
      { type: LegalDocType.PrivacyPolicy, version: 'v2', content: 'PP content' },
      { id: 'admin-3' }
    );

    const accepted = await service.accept(
      { docType: LegalDocType.PrivacyPolicy },
      { id: 'user-2', orgId: 'org-2' },
      { ipAddress: '127.0.0.1', userAgent: 'jest' }
    );
    assert.equal(accepted.version, 'v2');

    const again = await service.accept({ docType: LegalDocType.PrivacyPolicy }, { id: 'user-2', orgId: 'org-2' });
    const total = await acceptanceModel.countDocuments({ userId: 'user-2' });

    assert.equal(total, 1);
    assert.equal(again.version, 'v2');
    assert.equal(auditLog.calls.some((c) => c.eventType === 'legal.accepted'), true);
  });
});
