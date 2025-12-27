import test from 'node:test';
import assert from 'node:assert/strict';
import { IngestionContactsService } from '../src/features/ingestion/ingestion-contacts.service';
import { IngestionContactsProfile } from '../src/features/ingestion/ingestion.types';

class FakeAiService {
  enabled = false;
  isEnabled() {
    return this.enabled;
  }
  async suggestContactsMapping() {
    return { displayName: 'Full Name' };
  }
  async parseContactsRow() {
    return [
      {
        splitIndex: 0,
        extractedFrom: ['ai'],
        method: 'ai' as const,
        confidence: 0.9,
        person: { displayName: 'AI Person', emails: ['ai@example.com'] },
        issues: [],
      },
    ];
  }
  async enrichContactsCandidate() {
    return { person: { displayName: 'Enriched Name' }, companyName: 'Enriched Co' };
  }
}

class FakeAuditLogService {
  async log() {}
}

test('suggestMapping returns deterministic header picks', () => {
  const ai = new FakeAiService() as any;
  const audit = new FakeAuditLogService() as any;
  const svc = new IngestionContactsService(ai, audit);
  const res = svc.suggestMapping(
    { orgId: 'o1' },
    { headers: ['Full Name', 'Email Address', 'Company Name'], profile: IngestionContactsProfile.Mixed }
  );
  assert.equal(res.profile, IngestionContactsProfile.Mixed);
  assert.equal(res.suggestions.displayName, 'Full Name');
  assert.equal(res.suggestions.emails, 'Email Address');
  assert.equal(res.suggestions.companyName, 'Company Name');
});

test('parseRow splits on multiple emails and flags missing identifiers', async () => {
  const ai = new FakeAiService() as any;
  const audit = new FakeAuditLogService() as any;
  const svc = new IngestionContactsService(ai, audit);
  const res = await svc.parseRow(
    { orgId: 'o1' },
    {
      profile: IngestionContactsProfile.Mixed,
      cells: {
        name: 'John Doe; Jane Smith',
        contact: 'john@example.com; jane@example.com',
      },
      allowAiProcessing: false,
    }
  );

  assert.equal(res.mode, 'deterministic');
  assert.equal(res.candidates.length, 2);
  assert.equal(res.candidates[0].person.emails?.[0], 'john@example.com');
  assert.equal(res.candidates[1].person.emails?.[0], 'jane@example.com');
});

test('parseRow calls AI only when allowAiProcessing and AI enabled', async () => {
  const ai = new FakeAiService() as any;
  ai.enabled = true;
  const audit = new FakeAuditLogService() as any;
  const svc = new IngestionContactsService(ai, audit);
  const res = await svc.parseRow(
    { orgId: 'o1' },
    {
      profile: IngestionContactsProfile.Mixed,
      cells: { name: 'X', email: 'x@example.com' },
      allowAiProcessing: true,
    }
  );
  assert.equal(res.mode, 'ai');
  assert.equal(res.candidates[0].method, 'ai');
});

test('enrich returns deterministic suggestions without AI', async () => {
  const ai = new FakeAiService() as any;
  const audit = new FakeAuditLogService() as any;
  const svc = new IngestionContactsService(ai, audit);

  const res = await svc.enrich(
    { orgId: 'o1' },
    {
      profile: IngestionContactsProfile.Mixed,
      candidate: { person: { emails: ['john.doe@acme.com'] } },
      allowAiProcessing: false,
    } as any
  );

  assert.equal(res.mode, 'deterministic');
  assert.equal(res.suggestions.person.displayName, 'John Doe');
  assert.equal(res.suggestions.companyName, 'Acme');
});

test('enrich calls AI when allowAiProcessing and AI enabled', async () => {
  const ai = new FakeAiService() as any;
  ai.enabled = true;
  const audit = new FakeAuditLogService() as any;
  const svc = new IngestionContactsService(ai, audit);

  const res = await svc.enrich(
    { orgId: 'o1', userId: 'u1' },
    {
      profile: IngestionContactsProfile.Mixed,
      candidate: { person: { emails: ['x@example.com'] } },
      allowAiProcessing: true,
    } as any
  );

  assert.equal(res.mode, 'ai');
  assert.equal(res.suggestions.person.displayName, 'Enriched Name');
});
