import { describe, it } from 'node:test';
import { strict as assert } from 'node:assert';
import rateLimit from 'express-rate-limit';
import { getRateLimitExceededBody } from '../src/features/bulk/bulk.rate-limit.ts';

const createLimiter = () =>
  rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    standardHeaders: false,
    legacyHeaders: false,
    message: getRateLimitExceededBody(),
  });

const runMiddleware = (limiter: any, req: any) =>
  new Promise<{ statusCode: number; body: any }>((resolve, reject) => {
    const request: any = {
      method: 'GET',
      url: '/bulk-import/users',
      headers: {},
      app: { get: () => false },
      ip: req?.ip,
      ips: req?.ips || [],
      socket: { remoteAddress: req?.ip },
      connection: { remoteAddress: req?.ip },
      ...req,
    };
    request.headers = request.headers || {};
    request.app =
      request.app && typeof request.app.get === 'function' ? request.app : { get: () => false };
    request.connection = request.connection || { remoteAddress: request.ip };
    request.socket = request.socket || { remoteAddress: request.ip };
    request.ip = request.ip || request.connection?.remoteAddress || request.socket?.remoteAddress || '127.0.0.1';
    request.get =
      request.get ||
      function (header: string) {
        const key = header.toLowerCase();
        return this.headers[key] || this.headers[header] || undefined;
      };

    const res: any = {
      statusCode: 200,
      body: undefined,
      headers: {},
      headersSent: false,
      setHeader(key: string, value: any) {
        this.headers[key] = value;
      },
      append(key: string, value: any) {
        const current = this.headers[key];
        if (current === undefined) {
          this.headers[key] = value;
        } else if (Array.isArray(current)) {
          current.push(value);
        } else {
          this.headers[key] = [current, value];
        }
      },
      status(code: number) {
        this.statusCode = code;
        return this;
      },
      json(body: any) {
        this.body = body;
        resolve({ statusCode: this.statusCode, body });
      },
      send(body: any) {
        this.body = body;
        resolve({ statusCode: this.statusCode, body });
      },
    };
    try {
      limiter(request, res, (err: any) => {
        if (err) return reject(err);
        resolve({ statusCode: res.statusCode, body: res.body });
      });
    } catch (err) {
      reject(err);
    }
  });

describe('Bulk rate limit middleware', () => {
  it('allows 10 requests then returns 429 with standard body', async () => {
    const limiter = createLimiter();
    const ip = '127.0.0.1';

    for (let i = 0; i < 10; i++) {
      const result = await runMiddleware(limiter, { ip });
      assert.equal(result.statusCode, 200);
    }

    const overLimit = await runMiddleware(limiter, { ip });
    assert.equal(overLimit.statusCode, 429);
    assert.deepEqual(overLimit.body, getRateLimitExceededBody());
  });
});
