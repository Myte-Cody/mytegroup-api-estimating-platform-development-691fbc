import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { ValidationPipe } from '@nestjs/common';
import * as session from 'express-session';
import { clientOrigins, sessionConfig } from './config/app.config';
import rateLimit from 'express-rate-limit';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.use(
    session({
      ...sessionConfig,
    })
  );
  app.use(
    '/invites',
    rateLimit({
      windowMs: 15 * 60 * 1000,
      max: 20,
      standardHeaders: true,
      legacyHeaders: false,
    })
  );
  app.enableCors({ origin: clientOrigins(), credentials: true });
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true }));
  const port = process.env.PORT || 80;
  await app.listen(port);
  console.log(`Application running on http://localhost:${port}`);
}
bootstrap();
