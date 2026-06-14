/**
 * TA12 — Financeiro do motorista e solicitação de pagamento
 *
 * Verifica que após corridas finalizadas o saldo do motorista é consultável,
 * que o motorista pode criar uma solicitação de pagamento e que o
 * administrador consegue listar e confirmar essas solicitações.
 *
 * Documentação: Tabela 46 — Caso de teste de aceitação 12
 * Sistemas envolvidos: Mobile App (Motorista) ↔ MobU API; Painel Admin ↔ MobU API
 * Interface: GET /driver/financial/balance; POST /driver/financial/payment-request;
 *            GET /driver/financial/payment-requests;
 *            GET /admin/financial/payment-requests (admin);
 *            PATCH /admin/financial/payment-requests/:id/confirm (admin)
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedDriver,
  seedPassenger,
  seedAdmin,
  seedRegion,
  cleanupUsers,
  cleanupRegion,
  PARA_DE_MINAS,
  HOSPITAL,
  SeededDriver,
  SeededPassenger,
  SeededAdmin,
  SeededRegion,
} from '../helpers/seed';

describe('TA12 — Financeiro do motorista e solicitação de pagamento', () => {
  let ctx: TestApp;
  let driver: SeededDriver;
  let passenger: SeededPassenger;
  let admin: SeededAdmin;
  let region: SeededRegion;
  let driverToken: string;
  let passengerToken: string;
  let adminToken: string;
  let paymentRequestId: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    driver = await seedDriver(ctx.prisma, '+5531920120101');
    passenger = await seedPassenger(ctx.prisma, '+5531920120102');
    admin = await seedAdmin(ctx.prisma, '+5531920120103');

    const [dl, pl, al] = await Promise.all([
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: driver.phone, password: driver.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: admin.phone, password: admin.password }),
    ]);

    driverToken = dl.body.accessToken;
    passengerToken = pl.body.accessToken;
    adminToken = al.body.accessToken;

    // Realiza corrida completa para gerar saldo
    const rideRes = await request(ctx.app.getHttpServer())
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
      });

    const rideId = rideRes.body.id;

    await request(ctx.app.getHttpServer())
      .post(`/driver/rides/${rideId}/accept`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/arriving`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/arrived`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/start`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/finish`)
      .set('Authorization', `Bearer ${driverToken}`);
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [driver.id, passenger.id, admin.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  it('GET /driver/financial/balance deve retornar saldo positivo após corrida finalizada', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/driver/financial/balance')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(res.body).toHaveProperty('balanceCents');
    expect(typeof res.body.balanceCents).toBe('number');
  });

  it('motorista deve criar solicitação de pagamento com valor válido', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/driver/financial/payment-request')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ amountCents: 100, notes: 'Saque TA12' })
      .expect(201);

    expect(res.body).toHaveProperty('id');
    expect(res.body.status).toBe('PENDING');
    paymentRequestId = res.body.id;
  });

  it('GET /driver/financial/payment-requests deve listar a solicitação criada', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/driver/financial/payment-requests')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const ids = res.body.map((pr: { id: string }) => pr.id);
    expect(ids).toContain(paymentRequestId);
  });

  it('admin deve ver a solicitação em GET /admin/financial/payment-requests', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/admin/financial/payment-requests')
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const ids = res.body.map((pr: { id: string }) => pr.id);
    expect(ids).toContain(paymentRequestId);
  });

  it('admin deve confirmar a solicitação e mudar status para CONFIRMED', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/admin/financial/payment-requests/${paymentRequestId}/confirm`)
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(res.body).toHaveProperty('id');

    const pr = await ctx.prisma.driverPaymentRequest.findUnique({
      where: { id: paymentRequestId },
    });
    expect(pr!.status).toBe('CONFIRMED');
  });

  it('deve retornar 401 ao acessar saldo sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .get('/driver/financial/balance')
      .expect(401);
  });
});
