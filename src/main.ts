import * as dotenv from 'dotenv';
dotenv.config();
import { HttpAdapterHost, NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { ValidationPipe } from '@nestjs/common';
import session from 'express-session';
import { RedisStore } from 'connect-redis';
import { apiPort, apiOrigin, clientOrigins, sessionConfig } from './config/app.config';
import rateLimit from 'express-rate-limit';
import { AllExceptionsFilter } from './common/filters/all-exceptions.filter';
import { createLegacySessionClient } from './common/redis/redis.client';
import { redisConfig } from './config/redis.config';

async function bootstrap() {
  const app = await NestFactory.create(AppModule, { bufferLogs: true });
  const httpAdapter = app.get(HttpAdapterHost);
  app.useGlobalFilters(new AllExceptionsFilter(httpAdapter));
  const { sessionTtlHours } = redisConfig();
  const cookieMaxAge = sessionTtlHours * 60 * 60 * 1000;

  let store: session.Store | undefined;
  try {
    const redisClient = await createLegacySessionClient('session');
    store = new RedisStore({
      client: redisClient as any,
      prefix: 'sess:',
      ttl: sessionTtlHours * 60,
    });
  } catch (err) {
    // eslint-disable-next-line no-console
    console.warn('[session] Redis unavailable, falling back to in-memory store', err?.message || err);
  }

  app.use(
    session({
      ...sessionConfig,
      store,
      name: 'myte.sid',
      resave: false,
      saveUninitialized: false,
      cookie: {
        ...sessionConfig.cookie,
        maxAge: cookieMaxAge,
      },
      rolling: true,
    })
  );

  app.use((req, _res, next) => {
    if (req.session) {
      const now = new Date().toISOString();
      const meta = (req.session as any).meta || {};
      (req.session as any).meta = {
        ...meta,
        ip: req.ip,
        userAgent: req.headers['user-agent'] || '',
        createdAt: meta.createdAt || now,
        lastSeen: now,
      };
    }
    next();
  });
  app.use(
    '/invites',
    rateLimit({
      windowMs: 15 * 60 * 1000,
      max: 20,
      standardHeaders: true,
      legacyHeaders: false,
    })
  );
  app.use(
    ['/bulk-import', '/bulk-export'],
    rateLimit({
      windowMs: 15 * 60 * 1000,
      max: 10,
      standardHeaders: true,
      legacyHeaders: false,
    })
  );
  app.enableCors({ origin: clientOrigins(), credentials: true });
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true }));
  const port = apiPort();
  await ensureDevPortAvailable(port);
  await app.listen(port);
  console.log(`Application running on ${apiOrigin}`);
}
bootstrap();

async function ensureDevPortAvailable(port: number) {
  if (process.env.NODE_ENV === 'production') {
    return;
  }
  try {
    const mod = await import('kill-port');
    const killPort = (mod as any).default ?? (mod as any);
    await killPort(port, 'tcp');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    if (!message.toLowerCase().includes('no process')) {
      console.warn(`Attempted to free dev port ${port} but hit: ${message}`);
    }
  }
}
