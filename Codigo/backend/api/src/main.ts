import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { AppModule } from './app.module';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import type { NextFunction, Request, Response } from 'express';
import { RealtimeService } from './realtime/realtime.service';
import { PrismaExceptionFilter } from './common/prisma-exception.filter';
import { join } from 'path';

function setSecurityHeaders(req: Request, res: Response, next: NextFunction) {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Referrer-Policy', 'no-referrer');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  next();
}

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  app.useStaticAssets(join(process.cwd(), 'uploads'), { prefix: '/uploads' });
  const config = app.get(ConfigService);
  const httpAdapter = app.getHttpAdapter().getInstance();
  const allowedOrigins = config
    .get<string>('CORS_ORIGIN', 'http://localhost:3001,http://127.0.0.1:3001')
    .split(',')
    .map((origin) => origin.trim())
    .filter(Boolean);

  httpAdapter.disable?.('x-powered-by');
  app.use(setSecurityHeaders);
  app.enableCors({
    origin: (origin, callback) => {
      if (!origin || allowedOrigins.includes(origin)) {
        callback(null, true);
        return;
      }

      callback(new Error('Origem não permitida pelo CORS'));
    },
    methods: ['GET', 'POST', 'PATCH', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
  });

  app.useGlobalFilters(new PrismaExceptionFilter());
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      transformOptions: { enableImplicitConversion: true },
    }),
  );

  app.get(RealtimeService).attach(app.getHttpServer());

  await app.listen(
    config.get<number>('PORT', 3000),
    config.get<string>('HOST', '0.0.0.0'),
  );
}
bootstrap();
