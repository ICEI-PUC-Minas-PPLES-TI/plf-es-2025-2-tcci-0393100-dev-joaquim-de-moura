/**
 * TI4 — Registro de pagamento PIX off-line
 *
 * Verifica que o motorista pode marcar o pagamento como não recebido e,
 * posteriormente, confirmar o recebimento. O status do pagamento deve ser
 * atualizado e o txId/payload Pix devem ser preservados para consulta.
 *
 * Documentação: Tabela 31 — Caso de teste de integração 4
 * Sistemas envolvidos: Mobile App (Passageiro/Motorista) ↔ MobU API
 * Interface: POST /payments/rides/{rideId}/not-received
 *            POST /payments/rides/{rideId}/confirm
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

describe('TI4 — Registro de pagamento PIX off-line', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let driverToken: string;
  let passengerToken: string;
  let rideId: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531910040101');
    driver = await seedDriver(ctx.prisma, '+5531910040102');

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

    // Cria corrida PIX e simula o fluxo completo até FINISHED
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
        paymentMethod: 'PIX',
      });

    rideId = rideRes.body.id;

    // Motorista aceita → chega → passageiro embarca → corrida finalizada
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

  // ─── Marcar pagamento como não recebido ───────────────────────────────────

  it('deve marcar pagamento como NOT_RECEIVED ao chamar POST /payments/rides/:id/not-received', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/payments/rides/${rideId}/not-received`)
      .set('Authorization', `Bearer ${driverToken}`)
      .send({})
      .expect(201);

    expect(res.body).toHaveProperty('paymentStatus');
    expect(res.body.paymentStatus).toBe('NOT_RECEIVED');

    // Confirma no banco
    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.paymentStatus).toBe('NOT_RECEIVED');
  });

  // ─── Confirmar recebimento ────────────────────────────────────────────────

  it('deve confirmar pagamento com status RECEIVED ao chamar POST /payments/rides/:id/confirm', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post(`/payments/rides/${rideId}/confirm`)
      .set('Authorization', `Bearer ${driverToken}`)
      .send({})
      .expect(201);

    expect(res.body.paymentStatus).toBe('RECEIVED');

    // Confirma no banco
    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });
    expect(ride!.paymentStatus).toBe('RECEIVED');
    expect(ride!.paymentConfirmedAt).not.toBeNull();
    expect(ride!.paymentConfirmedBy).toBe(driver.id);
  });

  // ─── Rastreabilidade do Pix ───────────────────────────────────────────────

  it('deve preservar txId e payload Pix nos detalhes da corrida após confirmação', async () => {
    const ride = await ctx.prisma.ride.findUnique({ where: { id: rideId } });

    // QR Code PIX é gerado ao finalizar corrida com método PIX
    // Pelo menos um dos campos deve estar presente para rastreabilidade
    const hasPixData =
      ride!.paymentTxId !== null || ride!.paymentPixPayload !== null;
    expect(hasPixData).toBe(true);
  });

  it('deve retornar 401 ao tentar confirmar pagamento sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .post(`/payments/rides/${rideId}/confirm`)
      .send({})
      .expect(401);
  });
});
