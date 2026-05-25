import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import { AppModule } from '../../src/app.module';
import { GoogleDirectionsService } from '../../src/maps/google-directions.service';
import { NotificationService } from '../../src/notification/notification.service';
import { PrismaService } from '../../src/prisma/prisma.service';

// Rota fictícia retornada pelo mock do Google Directions (Pará de Minas)
const MOCK_ROUTE = {
  distanceMeters: 3_500,
  durationSeconds: 480,
  encodedPolyline: 'mock_polyline_para_de_minas',
};

export interface TestApp {
  app: INestApplication;
  prisma: PrismaService;
  mockDirections: { getRoute: jest.Mock };
  mockNotification: { sendToUser: jest.Mock; sendToUsers: jest.Mock };
}

/**
 * Cria e inicializa a aplicação NestJS em modo de teste.
 * - GoogleDirectionsService é substituído por um mock (sem chamadas reais à API).
 * - NotificationService é substituído por um mock (sem chamadas ao FCM/Firebase).
 * - DATABASE_URL lida de TEST_DATABASE_URL ou DATABASE_URL do ambiente.
 */
export async function createTestApp(): Promise<TestApp> {
  // Garante variáveis de ambiente mínimas necessárias para o ConfigModule
  process.env.JWT_SECRET =
    process.env.JWT_SECRET ?? 'test-jwt-secret-min-32-chars-long-key!';
  process.env.GOOGLE_MAPS_API_KEY =
    process.env.GOOGLE_MAPS_API_KEY ?? 'test-google-maps-key';

  if (process.env.TEST_DATABASE_URL) {
    process.env.DATABASE_URL = process.env.TEST_DATABASE_URL;
  }

  const mockDirections = { getRoute: jest.fn().mockResolvedValue(MOCK_ROUTE) };
  const mockNotification = {
    sendToUser: jest.fn().mockResolvedValue(undefined),
    sendToUsers: jest.fn().mockResolvedValue(undefined),
  };

  const moduleFixture = await Test.createTestingModule({
    imports: [AppModule],
  })
    .overrideProvider(GoogleDirectionsService)
    .useValue(mockDirections)
    .overrideProvider(NotificationService)
    .useValue(mockNotification)
    .compile();

  const app = moduleFixture.createNestApplication();

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      transformOptions: { enableImplicitConversion: true },
    }),
  );

  await app.init();

  const prisma = moduleFixture.get(PrismaService);

  return { app, prisma, mockDirections, mockNotification };
}
