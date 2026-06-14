/**
 * TA5 — Revisão pública do motorista
 *
 * Verifica que o passageiro pode criar uma revisão escrita para o motorista
 * após o término da corrida, que ela fica visível na listagem pública do
 * motorista (GET /reviews/drivers/:driverId) e que duplicidade é bloqueada.
 *
 * Documentação: Tabela 39 — Caso de teste de aceitação 5
 * Sistemas envolvidos: Mobile App (Passageiro) ↔ MobU API
 * Interface: POST /reviews/rides/:rideId; GET /reviews/drivers/:driverId;
 *            GET /reviews/me
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

describe('TA5 — Revisão pública do motorista', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let passengerToken: string;
  let driverToken: string;
  let rideId: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531920050101');
    driver = await seedDriver(ctx.prisma, '+5531920050102');

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

    // Fluxo completo para corrida FINISHED
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
    rideId = rideRes.body.id;

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
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  it('passageiro deve criar revisão após corrida finalizada e receber 201', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/reviews/rides/${rideId}`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ rating: 5, comment: 'Motorista muito educado e pontual!' })
      .expect(201);

    expect(res.body).toHaveProperty('id');
    expect(res.body.rating).toBe(5);
    expect(res.body.driverId).toBe(driver.id);
    expect(res.body.passengerId).toBe(passenger.id);
  });

  it('GET /reviews/drivers/:driverId deve listar a revisão do motorista com média', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get(`/reviews/drivers/${driver.id}`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(200);

    expect(res.body).toHaveProperty('reviews');
    expect(res.body).toHaveProperty('averageRating');
    expect(res.body.totalReviews).toBeGreaterThanOrEqual(1);

    const rideIds = res.body.reviews.map((r: { rideId: string }) => r.rideId);
    expect(rideIds).toContain(rideId);
  });

  it('GET /reviews/me (passageiro) deve retornar as revisões feitas por ele', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/reviews/me')
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const rideIds = res.body.map((r: { rideId: string }) => r.rideId);
    expect(rideIds).toContain(rideId);
  });

  it('deve impedir segunda revisão da mesma corrida com 400', async () => {
    await request(ctx.app.getHttpServer())
      .post(`/reviews/rides/${rideId}`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ rating: 3, comment: 'Segunda tentativa' })
      .expect(400);
  });

  it('deve retornar 401 sem autenticação ao criar revisão', async () => {
    await request(ctx.app.getHttpServer())
      .post(`/reviews/rides/${rideId}`)
      .send({ rating: 4 })
      .expect(401);
  });
});
