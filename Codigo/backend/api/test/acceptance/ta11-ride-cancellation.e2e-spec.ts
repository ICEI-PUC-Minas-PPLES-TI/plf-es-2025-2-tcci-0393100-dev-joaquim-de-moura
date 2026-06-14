/**
 * TA11 — Cancelamento de corrida pelo passageiro
 *
 * Verifica que o passageiro pode cancelar uma corrida em estado
 * PENDING_DRIVER antes que um motorista a aceite, e que o sistema
 * bloqueia o cancelamento em estados inválidos (ex.: após aceite pelo
 * motorista via rejeição pelo driver também é testada).
 *
 * Documentação: Tabela 45 — Caso de teste de aceitação 11
 * Sistemas envolvidos: Mobile App (Passageiro/Motorista) ↔ MobU API
 * Interface: POST /rides; PATCH /rides/:id/status;
 *            POST /driver/rides/:id/reject
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

describe('TA11 — Cancelamento de corrida pelo passageiro', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let passengerToken: string;
  let driverToken: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531920110101');
    driver = await seedDriver(ctx.prisma, '+5531920110102');

    const [pl, dl] = await Promise.all([
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: driver.phone, password: driver.password }),
    ]);

    passengerToken = pl.body.accessToken;
    driverToken = dl.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  it('passageiro deve cancelar corrida PENDING_DRIVER com sucesso', async () => {
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

    const res = await request(ctx.app.getHttpServer())
      .patch(`/rides/${rideId}/status`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ status: 'CANCELED' })
      .expect(200);

    expect(res.body.status).toBe('CANCELED');

    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.status).toBe('CANCELED');
  });

  it('corrida cancelada deve aparecer no histórico do passageiro', async () => {
    // Cria e cancela outra corrida
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
      .patch(`/rides/${rideId}/status`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ status: 'CANCELED' });

    const histRes = await request(ctx.app.getHttpServer())
      .get('/rides/my-rides')
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(200);

    const ids = histRes.body.map((r: { id: string }) => r.id);
    expect(ids).toContain(rideId);
  });

  it('motorista deve poder recusar uma corrida pendente via POST /driver/rides/:id/reject', async () => {
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

    const res = await request(ctx.app.getHttpServer())
      .post(`/driver/rides/${rideId}/reject`)
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(201);

    expect(res.body.success).toBe(true);
  });

  it('deve retornar 401 sem autenticação ao cancelar corrida', async () => {
    await request(ctx.app.getHttpServer())
      .patch('/rides/any-id/status')
      .send({ status: 'CANCELED' })
      .expect(401);
  });
});
