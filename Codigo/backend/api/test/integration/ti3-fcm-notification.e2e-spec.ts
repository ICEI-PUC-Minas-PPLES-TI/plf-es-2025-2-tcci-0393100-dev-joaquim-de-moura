/**
 * TI3 — Notificação de nova corrida (FCM)
 *
 * Verifica que o token FCM é registrado corretamente pela API e que,
 * ao criar uma corrida com motorista online, o backend aciona o serviço
 * de notificações para o motorista.
 *
 * Resultado esperado na documentação: PARCIAL
 * — O recebimento visual da push notification não pode ser comprovado em emulador.
 * — Este teste valida a camada de integração: registro do token e acionamento do serviço.
 *
 * Documentação: Tabela 30 — Caso de teste de integração 3
 * Sistemas envolvidos: MobU API ↔ FCM (mock) ↔ Mobile App (Motorista)
 * Interface: PATCH /auth/fcm-token; POST /rides (dispara notificação)
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedPassenger,
  seedDriver,
  seedRegion,
  cleanupUsers,
  cleanupRegion,
  PARA_DE_MINAS,
  HOSPITAL,
  SeededPassenger,
  SeededDriver,
  SeededRegion,
} from '../helpers/seed';

describe('TI3 — Notificação de nova corrida (FCM)', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let driverToken: string;
  let passengerToken: string;

  const FAKE_FCM_TOKEN = 'fcm_test_token_ti3_motorista_qa_device_001';

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531910030101');
    driver = await seedDriver(ctx.prisma, '+5531910030102');

    const [driverLogin, passengerLogin] = await Promise.all([
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: driver.phone, password: driver.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
    ]);

    driverToken = driverLogin.body.accessToken;
    passengerToken = passengerLogin.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  // ─── Registro de token FCM ────────────────────────────────────────────────

  it('deve registrar token FCM do motorista com PATCH /auth/fcm-token e retornar 200', async () => {
    await request(ctx.app.getHttpServer())
      .patch('/auth/fcm-token')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ token: FAKE_FCM_TOKEN })
      .expect(200);

    // Verifica que o token foi persistido no banco
    const user = await ctx.prisma.user.findUnique({
      where: { id: driver.id },
    });
    expect(user!.fcmToken).toBe(FAKE_FCM_TOKEN);
  });

  it('deve retornar 401 ao tentar registrar FCM token sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .patch('/auth/fcm-token')
      .send({ token: FAKE_FCM_TOKEN })
      .expect(401);
  });

  // ─── Acionamento do serviço de notificação ────────────────────────────────

  it('deve acionar NotificationService ao criar corrida com motorista online disponível', async () => {
    ctx.mockNotification.sendToUser.mockClear();

    await request(ctx.app.getHttpServer())
      .post('/rides')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({
        originLat: PARA_DE_MINAS.lat,
        originLng: PARA_DE_MINAS.lng,
        destLat: HOSPITAL.lat,
        destLng: HOSPITAL.lng,
        originAddress: PARA_DE_MINAS.addr,
        destinationAddress: HOSPITAL.addr,
        paymentMethod: 'CASH',
      })
      .expect(201);

    // O backend chama sendToUsers (plural) para notificar motoristas online disponíveis
    expect(ctx.mockNotification.sendToUsers).toHaveBeenCalled();
    const [calledUserIds] = ctx.mockNotification.sendToUsers.mock.calls[0];
    expect(Array.isArray(calledUserIds)).toBe(true);
    expect(calledUserIds).toContain(driver.id);
  });

  it('deve ter persistido o token FCM antes de acionar a notificação', async () => {
    const user = await ctx.prisma.user.findUnique({ where: { id: driver.id } });
    expect(user!.fcmToken).toBe(FAKE_FCM_TOKEN);
  });
});
