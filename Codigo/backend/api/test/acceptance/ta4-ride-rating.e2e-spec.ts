/**
 * TA4 — Avaliação de corrida pelo passageiro
 *
 * Verifica que o passageiro pode avaliar o motorista após o término
 * da corrida, que a nota fica registrada e é consultável via GET /ratings/me
 * (por parte do motorista), e que tentativas de avaliar corridas não
 * finalizadas ou já avaliadas são rejeitadas.
 *
 * Documentação: Tabela 38 — Caso de teste de aceitação 4
 * Sistemas envolvidos: Mobile App (Passageiro/Motorista) ↔ MobU API
 * Interface: POST /ratings; GET /ratings/me
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

describe('TA4 — Avaliação de corrida pelo passageiro', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let passengerToken: string;
  let driverToken: string;
  let pendingRideId: string;
  let finishedRideId: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531920040101');
    driver = await seedDriver(ctx.prisma, '+5531920040102');

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

    // Corrida pendente (para testar avaliação antecipada)
    const pendingRes = await request(ctx.app.getHttpServer())
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
    pendingRideId = pendingRes.body.id;

    // Corrida finalizada (fluxo completo)
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
    finishedRideId = rideRes.body.id;

    await request(ctx.app.getHttpServer())
      .post(`/driver/rides/${finishedRideId}/accept`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${finishedRideId}/arriving`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${finishedRideId}/arrived`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${finishedRideId}/start`)
      .set('Authorization', `Bearer ${driverToken}`);
    await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${finishedRideId}/finish`)
      .set('Authorization', `Bearer ${driverToken}`);
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  it('deve rejeitar avaliação de corrida ainda PENDING_DRIVER com 400', async () => {
    await request(ctx.app.getHttpServer())
      .post('/ratings')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ rideId: pendingRideId, score: 5 })
      .expect(400);
  });

  it('deve rejeitar nota fora do intervalo 1–5 com 400', async () => {
    await request(ctx.app.getHttpServer())
      .post('/ratings')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ rideId: finishedRideId, score: 6 })
      .expect(400);
  });

  it('passageiro deve avaliar a corrida finalizada com nota 5 e comentário', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/ratings')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ rideId: finishedRideId, score: 5, comment: 'Excelente motorista!' })
      .expect(201);

    expect(res.body).toHaveProperty('id');
    expect(res.body.score).toBe(5);
    expect(res.body.ratedUserId).toBe(driver.id);
  });

  it('deve impedir segunda avaliação da mesma corrida com 400', async () => {
    await request(ctx.app.getHttpServer())
      .post('/ratings')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ rideId: finishedRideId, score: 3 })
      .expect(400);
  });

  it('GET /ratings/me (motorista) deve conter a avaliação recebida com média calculada', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/ratings/me')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(res.body).toHaveProperty('ratings');
    expect(res.body).toHaveProperty('average');
    expect(res.body.total).toBeGreaterThanOrEqual(1);

    const rideIds = res.body.ratings.map((r: { rideId: string }) => r.rideId);
    expect(rideIds).toContain(finishedRideId);
  });
});
