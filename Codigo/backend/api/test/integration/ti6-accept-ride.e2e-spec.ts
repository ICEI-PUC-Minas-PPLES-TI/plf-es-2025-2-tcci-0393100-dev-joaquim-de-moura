/**
 * TI6 — Aceitar corrida e iniciar rota
 *
 * Verifica o fluxo completo do motorista: consultar corridas pendentes,
 * aceitar uma corrida, percorrer todos os estados de status (ACCEPTED →
 * DRIVER_ARRIVING → DRIVER_ARRIVED → IN_PROGRESS → FINISHED) e confirmar
 * que a API de Mapas (mockada) fornece o trajeto até a origem.
 *
 * Documentação: Tabela 33 — Caso de teste de integração 6
 * Sistemas envolvidos: Mobile App (Motorista) ↔ MobU API ↔ API de Mapas/Geo (mock)
 * Interface: GET /driver/rides/pending; POST /driver/rides/:id/accept;
 *            PATCH /driver/rides/:id/arriving | arrived | start | finish
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

describe('TI6 — Aceitar corrida e iniciar rota', () => {
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
    passenger = await seedPassenger(ctx.prisma, '+5531910060101');
    driver = await seedDriver(ctx.prisma, '+5531910060102');

    const [passengerLogin, driverLogin] = await Promise.all([
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: driver.phone, password: driver.password }),
    ]);

    passengerToken = passengerLogin.body.accessToken;
    driverToken = driverLogin.body.accessToken;

    // Passageiro solicita corrida
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
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  // ─── Passo 1: Motorista consulta corridas pendentes ───────────────────────

  it('deve listar a corrida pendente em GET /driver/rides/pending para motorista online', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/driver/rides/pending')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const ids = res.body.map((r: { rideId: string }) => r.rideId);
    expect(ids).toContain(rideId);
  });

  // ─── Passo 2: Motorista aceita a corrida ──────────────────────────────────

  it('deve mudar status da corrida para ACCEPTED ao chamar POST /driver/rides/:id/accept', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/driver/rides/${rideId}/accept`)
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(201);

    expect(res.body.status).toBe('ACCEPTED');

    // Confirma no banco (o response usa rideId, não id; driverId é verificado via DB)
    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.status).toBe('ACCEPTED');
    expect(ride!.driverId).toBe(driver.id);
  });

  it('deve ter consultado a API de Mapas ao aceitar a corrida para calcular rota', () => {
    // GoogleDirectionsService.getRoute deve ter sido chamado durante o aceite
    expect(ctx.mockDirections.getRoute).toHaveBeenCalled();
  });

  // ─── Passo 3: Motorista a caminho ─────────────────────────────────────────

  it('deve mudar status para DRIVER_ARRIVING ao chamar PATCH /driver/rides/:id/arriving', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/arriving`)
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(res.body.status).toBe('DRIVER_ARRIVING');

    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.status).toBe('DRIVER_ARRIVING');
    expect(ride!.driverArrivingAt).not.toBeNull();
  });

  // ─── Passo 4: Motorista chegou ao local de embarque ──────────────────────

  it('deve mudar status para DRIVER_ARRIVED ao chamar PATCH /driver/rides/:id/arrived', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/arrived`)
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(res.body.status).toBe('DRIVER_ARRIVED');

    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.status).toBe('DRIVER_ARRIVED');
    expect(ride!.driverArrivedAt).not.toBeNull();
  });

  // ─── Passo 5: Passageiro embarca — corrida em andamento ──────────────────

  it('deve mudar status para IN_PROGRESS ao chamar PATCH /driver/rides/:id/start', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/start`)
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(res.body.status).toBe('IN_PROGRESS');

    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.status).toBe('IN_PROGRESS');
    expect(ride!.startedAt).not.toBeNull();
  });

  // ─── Passo 6: Motorista finaliza a corrida ────────────────────────────────

  it('deve mudar status para FINISHED ao chamar PATCH /driver/rides/:id/finish', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/driver/rides/${rideId}/finish`)
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(res.body.status).toBe('FINISHED');

    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.status).toBe('FINISHED');
    expect(ride!.finishedAt).not.toBeNull();
  });

  // ─── Histórico ────────────────────────────────────────────────────────────

  it('deve aparecer no histórico do motorista após ser finalizada', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/driver/rides/history')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    const ids = res.body.rides.map((r: { id: string }) => r.id);
    expect(ids).toContain(rideId);
  });

  it('deve aparecer no histórico do passageiro após ser finalizada', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/rides/my-rides')
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(200);

    const ids = res.body.map((r: { id: string }) => r.id);
    expect(ids).toContain(rideId);
  });
});
