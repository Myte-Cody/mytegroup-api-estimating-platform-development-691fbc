import test from 'node:test';
import assert from 'node:assert/strict';
import { computeWithinFileDuplicateErrors } from '../src/features/people/people-import-duplicates.util';

test('flags duplicate primaryEmail within file', () => {
  const errors = computeWithinFileDuplicateErrors([
    { row: 2, emails: ['ALICE@EXAMPLE.COM'] },
    { row: 3, primaryEmail: 'alice@example.com', emails: ['alice@example.com'] },
    { row: 4, emails: ['bob@example.com'] },
  ]);

  assert.deepEqual(errors.get(2), ['duplicate primaryEmail within file (rows 2, 3)']);
  assert.deepEqual(errors.get(3), ['duplicate primaryEmail within file (rows 2, 3)']);
  assert.equal(errors.has(4), false);
});

test('flags duplicate ironworkerNumber within file', () => {
  const errors = computeWithinFileDuplicateErrors([
    { row: 10, ironworkerNumber: '12345' },
    { row: 11, ironworkerNumber: '12345' },
  ]);

  assert.deepEqual(errors.get(10), ['duplicate ironworkerNumber within file (rows 10, 11)']);
  assert.deepEqual(errors.get(11), ['duplicate ironworkerNumber within file (rows 10, 11)']);
});

test('flags duplicate primaryPhone within file (normalized)', () => {
  const errors = computeWithinFileDuplicateErrors([
    { row: 20, primaryPhone: '(555) 111-2222' },
    { row: 21, phones: ['555-111-2222'] },
  ]);

  assert.deepEqual(errors.get(20), ['duplicate primaryPhone within file (rows 20, 21)']);
  assert.deepEqual(errors.get(21), ['duplicate primaryPhone within file (rows 20, 21)']);
});

