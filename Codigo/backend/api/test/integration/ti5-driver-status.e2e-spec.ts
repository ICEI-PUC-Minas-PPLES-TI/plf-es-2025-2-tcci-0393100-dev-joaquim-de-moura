/**
 * TI5 — Alternar status online/off-line do motorista
 *
 * Verifica que o aplicativo aciona a API corretamente ao alternar o status
 * do motorista entre online e offline, e que o estado atualizado é refletido
 * no banco de dados e impacta a listagem de corridas disponíveis.
 *
 * Documentação: Tabela 32 — Caso de teste de integração 5
 * Sistemas envolvidos: Mobile App (Motorista) ↔ MobU API
 * Interface: POST /driver/status
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedDriver,
  seedPassenger,
  seedRegion,
  cleanupUsers,
  cleanupRegion,
  PARA_DE_MINAS,
  HOSPITAL,
  SeededDriver,
  SeededPassenger,
  SeededRegion,
} from '../helpers/seed';

describe('TI5 — Alternar status online/off-line do motorista', () => {
  let ctx: TestApp;
  let driver: SeededDriver;
  let passenger: SeededPassenger;
  let region: SeededRegion;
  let driverToken: string;
  let passengerToken: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    driver = await seedDriver(ctx.prisma, '+5531910050101');
    passenger = await seedPassenger(ctx.prisma, '+5531910050102');

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
    await cleanupUsers(ctx.prisma, [driver.id, passenger.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  // ─── Ficar offline ────────────────────────────────────────────────────────

  it('deve colocar o motorista offline ao chamar POST /driver/status com online=false', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/driver/status')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ online: false })
      .expect(201);

    expect(res.body).toHaveProperty('online');
    expect(res.body.online).toBe(false);

    // Confirma no banco
    const profile = await ctx.prisma.driverProfile.findUnique({
      where: { userId: driver.id },
    });
    expect(profile!.online).toBe(false);
    // Offline deve forçar available=false
    expect(profile!.available).toBe(false);
  });

  it('deve excluir o motorista offline da listagem GET /driver/available', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/driver/available')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    const driverIds = res.body.map((d: { userId: string }) => d.userId);
    expect(driverIds).not.toContain(driver.id);
  });

  // ─── Ficar online ─────────────────────────────────────────────────────────

  it('deve colocar o motorista online ao chamar POST /driver/status com online=true', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/driver/status')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ online: true, available: true })
      .expect(201);

    expect(res.body.online).toBe(true);

    // Confirma no banco
    const profile = await ctx.prisma.driverProfile.findUnique({
      where: { userId: driver.id },
    });
    expect(profile!.online).toBe(true);
  });

  // ─── Impacto na distribuição de corridas ──────────────────────────────────

  it('deve tornar motorista online visível para corridas pendentes após ir para online', async () => {
    // Cria uma corrida para o passageiro
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
      })
      .expect(201);

    // Motorista online deve ver a corrida em GET /driver/rides/pending
    const pendingRes = await request(ctx.app.getHttpServer())
      .get('/driver/rides/pending')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    const rideIds = pendingRes.body.map((r: { rideId: string }) => r.rideId);
    expect(rideIds).toContain(rideRes.body.id);
  });

  it('deve retornar 401 ao tentar alterar status sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .post('/driver/status')
      .send({ online: true })
      .expect(401);
  });
});
