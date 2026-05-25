/**
 * TI2 — Solicitação de corrida com rota
 *
 * Verifica que a API processa origem e destino, consulta a API de Mapas (mockada)
 * e retorna estimativa de valor, tempo e distância ao passageiro.
 * Em seguida, a criação da corrida é validada com status PENDING_DRIVER e
 * registro persistido no banco.
 *
 * Documentação: Tabela 29 — Caso de teste de integração 2
 * Sistemas envolvidos: Mobile App (Passageiro) ↔ MobU API ↔ API de Mapas/Geo (mock)
 * Interface: POST /rides/estimate; POST /rides
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedPassenger,
  seedRegion,
  cleanupUsers,
  cleanupRegion,
  PARA_DE_MINAS,
  HOSPITAL,
  SeededPassenger,
  SeededRegion,
} from '../helpers/seed';

describe('TI2 — Solicitação de corrida com rota', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let region: SeededRegion;
  let passengerToken: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531910020101');

    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: passenger.password });

    passengerToken = loginRes.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  // ─── Estimativa ───────────────────────────────────────────────────────────

  it('deve retornar estimativa com valor, distância e duração ao chamar POST /rides/estimate', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/rides/estimate')
      .send({
        originLat: PARA_DE_MINAS.lat,
        originLng: PARA_DE_MINAS.lng,
        destLat: HOSPITAL.lat,
        destLng: HOSPITAL.lng,
      })
      .expect(201);

    expect(res.body).toHaveProperty('estimatedFareCents');
    expect(typeof res.body.estimatedFareCents).toBe('number');
    expect(res.body.estimatedFareCents).toBeGreaterThan(0);

    expect(res.body).toHaveProperty('distanceMeters');
    expect(res.body.distanceMeters).toBeGreaterThan(0);

    expect(res.body).toHaveProperty('durationSeconds');
    expect(res.body.durationSeconds).toBeGreaterThan(0);
  });

  it('deve ter chamado GoogleDirectionsService.getRoute ao estimar a corrida', () => {
    expect(ctx.mockDirections.getRoute).toHaveBeenCalledWith(
      PARA_DE_MINAS.lat,
      PARA_DE_MINAS.lng,
      HOSPITAL.lat,
      HOSPITAL.lng,
      expect.any(String),
    );
  });

  // ─── Criação da corrida ───────────────────────────────────────────────────

  it('deve criar corrida com status PENDING_DRIVER e persistir no banco ao chamar POST /rides', async () => {
    const res = await request(ctx.app.getHttpServer())
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

    expect(res.body).toHaveProperty('id');
    expect(res.body.status).toBe('PENDING_DRIVER');
    expect(res.body.paymentMethod).toBe('CASH');

    // Confirma que a corrida foi persistida no banco
    const rideInDb = await ctx.prisma.ride.findUnique({
      where: { id: res.body.id },
    });
    expect(rideInDb).not.toBeNull();
    expect(rideInDb!.passengerId).toBe(passenger.id);
  });

  it('deve criar corrida com método de pagamento PIX', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/rides')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({
        originLat: PARA_DE_MINAS.lat,
        originLng: PARA_DE_MINAS.lng,
        destLat: HOSPITAL.lat,
        destLng: HOSPITAL.lng,
        originAddress: PARA_DE_MINAS.addr,
        destinationAddress: HOSPITAL.addr,
        paymentMethod: 'PIX',
      })
      .expect(201);

    expect(res.body.paymentMethod).toBe('PIX');
    expect(res.body.status).toBe('PENDING_DRIVER');
  });

  it('deve retornar 401 ao tentar criar corrida sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .post('/rides')
      .send({
        originLat: PARA_DE_MINAS.lat,
        originLng: PARA_DE_MINAS.lng,
        destLat: HOSPITAL.lat,
        destLng: HOSPITAL.lng,
        paymentMethod: 'CASH',
      })
      .expect(401);
  });
});
