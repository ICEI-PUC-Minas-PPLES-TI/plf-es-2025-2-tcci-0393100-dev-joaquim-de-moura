/**
 * TA7 — Funcionalidades de segurança durante corrida
 *
 * Verifica que passageiro e motorista podem acionar as funções de segurança
 * (compartilhar viagem, botão de emergência, gravação de áudio) durante
 * uma corrida em andamento, e que os eventos ficam registrados no log de
 * auditoria. Terceiros são bloqueados.
 *
 * Documentação: Tabela 41 — Caso de teste de aceitação 7
 * Sistemas envolvidos: Mobile App ↔ MobU API
 * Interface: POST /rides/:id/safety/share; POST /rides/:id/safety/emergency;
 *            POST /rides/:id/safety/audio; GET /rides/:id/safety/audit
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

describe('TA7 — Funcionalidades de segurança durante corrida', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let outsider: SeededPassenger;
  let region: SeededRegion;
  let passengerToken: string;
  let driverToken: string;
  let outsiderToken: string;
  let rideId: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531920070101');
    driver = await seedDriver(ctx.prisma, '+5531920070102');
    outsider = await seedPassenger(ctx.prisma, '+5531920070103');

    const [pl, dl, ol] = await Promise.all([
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: driver.phone, password: driver.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: outsider.phone, password: outsider.password }),
    ]);

    passengerToken = pl.body.accessToken;
    driverToken = dl.body.accessToken;
    outsiderToken = ol.body.accessToken;

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
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id, outsider.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  it('passageiro deve compartilhar viagem e receber confirmação', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/rides/${rideId}/safety/share`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ channel: 'whatsapp' })
      .expect(201);

    expect(res.body.ok).toBe(true);
  });

  it('passageiro deve acionar botão de emergência e receber números de emergência', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/rides/${rideId}/safety/emergency`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ note: 'Situação suspeita', lat: PARA_DE_MINAS.lat, lng: PARA_DE_MINAS.lng })
      .expect(201);

    expect(res.body.ok).toBe(true);
    expect(res.body.emergencyNumbers).toContain('190');
    expect(res.body.emergencyNumbers).toContain('192');
  });

  it('motorista deve iniciar gravação de áudio com sucesso', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/rides/${rideId}/safety/audio`)
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ phase: 'start', consentAccepted: true })
      .expect(201);

    expect(res.body.ok).toBe(true);
  });

  it('GET /rides/:id/safety/audit deve listar todos os eventos registrados', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get(`/rides/${rideId}/safety/audit`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const actions = res.body.map((e: { action: string }) => e.action);
    expect(actions).toContain('SHARE_TRIP');
    expect(actions).toContain('EMERGENCY_TAP');
    expect(actions).toContain('AUDIO_RECORDING_START');
  });

  it('terceiro sem vínculo à corrida deve receber 403 ao acionar emergência', async () => {
    await request(ctx.app.getHttpServer())
      .post(`/rides/${rideId}/safety/emergency`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .send({})
      .expect(403);
  });

  it('deve retornar 401 sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .get(`/rides/${rideId}/safety/audit`)
      .expect(401);
  });
});
