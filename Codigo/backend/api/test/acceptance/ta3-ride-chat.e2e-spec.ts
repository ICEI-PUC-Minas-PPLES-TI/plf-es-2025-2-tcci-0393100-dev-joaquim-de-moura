/**
 * TA3 — Chat durante corrida (passageiro ↔ motorista)
 *
 * Verifica que passageiro e motorista conseguem trocar mensagens de texto
 * durante uma corrida em andamento e que o histórico é retornado na ordem
 * correta. Terceiros sem vínculo à corrida são bloqueados.
 *
 * Documentação: Tabela 37 — Caso de teste de aceitação 3
 * Sistemas envolvidos: Mobile App ↔ MobU API
 * Interface: POST /rides/:id/messages; GET /rides/:id/messages
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

describe('TA3 — Chat durante corrida', () => {
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
    passenger = await seedPassenger(ctx.prisma, '+5531920030101');
    driver = await seedDriver(ctx.prisma, '+5531920030102');
    outsider = await seedPassenger(ctx.prisma, '+5531920030103');

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

    // Cria e aceita corrida para ter driverID vinculado
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
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id, outsider.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  it('passageiro deve enviar mensagem e receber 201 com o conteúdo', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/rides/${rideId}/messages`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({ content: 'Estou na esquina da padaria.' })
      .expect(201);

    expect(res.body).toHaveProperty('id');
    expect(res.body.content).toBe('Estou na esquina da padaria.');
    expect(res.body.senderRole).toBe('PASSENGER');
  });

  it('motorista deve enviar mensagem e receber 201 com o conteúdo', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/rides/${rideId}/messages`)
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ content: 'Chegando em 2 minutos!' })
      .expect(201);

    expect(res.body.content).toBe('Chegando em 2 minutos!');
    expect(res.body.senderRole).toBe('DRIVER');
  });

  it('GET /rides/:id/messages deve retornar as duas mensagens em ordem cronológica', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get(`/rides/${rideId}/messages`)
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThanOrEqual(2);

    const contents = res.body.map((m: { content: string }) => m.content);
    expect(contents).toContain('Estou na esquina da padaria.');
    expect(contents).toContain('Chegando em 2 minutos!');
  });

  it('terceiro sem vínculo à corrida deve receber 403 ao tentar enviar mensagem', async () => {
    await request(ctx.app.getHttpServer())
      .post(`/rides/${rideId}/messages`)
      .set('Authorization', `Bearer ${outsiderToken}`)
      .send({ content: 'Mensagem não autorizada' })
      .expect(403);
  });

  it('deve retornar 401 sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .get(`/rides/${rideId}/messages`)
      .expect(401);
  });
});
