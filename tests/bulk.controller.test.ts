import { strict as assert } from 'node:assert';
import { describe, it } from 'node:test';
import { BadRequestException, HttpException, HttpStatus } from '@nestjs/common';
import { once } from 'node:events';
import { Readable, Writable } from 'stream';
import { BulkController } from '../src/features/bulk/bulk.controller.ts';
import { getRateLimitExceededBody } from '../src/features/bulk/bulk.rate-limit.ts';

const actor = { id: 'user-1', orgId: 'org-1', role: 'admin', roles: ['admin'] };

describe('BulkController', () => {
  it('forwards import payload, org, actor, and dryRun to service', async () => {
    const calls: any[] = [];
    const bulk = {
      import: async (...args: any[]) => {
        calls.push(args);
        return { ok: true };
      },
    };
    const controller = new BulkController(bulk as any);
    const req: any = { session: { user: actor } };
    const file: any = { originalname: 'contacts.csv', mimetype: 'text/csv', buffer: Buffer.from('') };

    const result = await controller.import(
      'contacts',
      file,
      undefined,
      { format: 'csv' },
      'true',
      undefined,
      req
    );

    assert.deepEqual(result, { ok: true });
    const [entityType, options] = calls[0] || [];
    assert.equal(entityType, 'contacts');
    assert.equal(options.dryRun, true);
    assert.equal(options.actor, actor);
    assert.equal(options.file, file);
  });

  it('rejects unsupported entity type and file mimetype', async () => {
    const controller = new BulkController({ import: async () => ({}) } as any);
    await assert.rejects(
      () => controller.import('widgets', undefined as any, undefined as any, {}, 'false', undefined, { session: { user: actor } } as any),
      BadRequestException
    );
    const badFile: any = { originalname: 'bad.pdf', mimetype: 'application/pdf', buffer: Buffer.from('') };
    await assert.rejects(
      () => controller.import('contacts', badFile, undefined as any, {}, 'false', undefined, { session: { user: actor } } as any),
      BadRequestException
    );
  });

  it('returns 429 when rate limit exceeded', async () => {
    const controller = new BulkController({ import: async () => ({}) } as any);
    const req: any = { session: { user: actor }, rateLimit: { current: 11, limit: 10 } };

    let err: any;
    try {
      await controller.import('contacts', undefined as any, undefined as any, {}, 'false', undefined, req);
    } catch (e) {
      err = e;
    }
    assert.equal(typeof err?.getStatus, 'function');
    const response = typeof err.getResponse === 'function' ? err.getResponse() : err?.response;
    assert.equal(err.getStatus(), HttpStatus.TOO_MANY_REQUESTS);
    assert.equal(response?.message, getRateLimitExceededBody().message);
  });

  it('returns JSON export and honors includeArchived flag', async () => {
    const calls: any[] = [];
    const bulk = {
      export: async (...args: any[]) => {
        calls.push(args);
        return { entityType: 'contacts', format: 'json', count: 2, data: [{}, {}] };
      },
    };
    const controller = new BulkController(bulk as any);
    const req: any = { session: { user: actor } };

    const result = await controller.export('contacts', 'json', 'true', undefined, req);

    assert.equal(result?.count, 2);
    const [entityType, options] = calls[0] || [];
    assert.equal(entityType, 'contacts');
    assert.equal(options.includeArchived, true);
  });

  it('pipes CSV export to response with headers set', async () => {
    let piped = '';
    const res: any = new (class extends Writable {
      headers: Record<string, string> = {};
      setHeader(key: string, val: string) {
        this.headers[key] = val;
      }
      _write(chunk: any, _enc: any, cb: any) {
        piped += chunk.toString();
        cb();
      }
    })();

    const bulk = {
      export: async () => ({
        entityType: 'contacts',
        format: 'csv',
        count: 1,
        filename: 'contacts.csv',
        stream: Readable.from(['col1,col2\nv1,v2\n']),
      }),
    };
    const controller = new BulkController(bulk as any);

    const done = once(res, 'finish');
    await controller.export('contacts', 'csv', 'false', undefined, { session: { user: actor } } as any, res as any);
    await done;

    assert.ok(piped.includes('col1,col2'));
    assert.equal(res.headers['Content-Type'], 'text/csv');
  });
});
