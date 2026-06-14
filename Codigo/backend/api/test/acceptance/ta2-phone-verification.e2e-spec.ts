/**
 * TA2 — Verificação de telefone via OTP
 *
 * Verifica que o passageiro consegue solicitar um código SMS, confirmar
 * com o código correto (capturado via devHint em ambiente de teste) e que
 * tentativas com código inválido são rejeitadas.
 *
 * Documentação: Tabela 36 — Caso de teste de aceitação 2
 * Sistemas envolvidos: Mobile App (Passageiro) ↔ MobU API
 * Interface: POST /auth/verification/phone/request;
 *            POST /auth/verification/phone/confirm
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import { seedPassenger, cleanupUsers, SeededPassenger } from '../helpers/seed';

describe('TA2 — Verificação de telefone via OTP', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let token: string;

  beforeAll(async () => {
    ctx = await createTestApp();
    passenger = await seedPassenger(ctx.prisma, '+5531920020101');

    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: passenger.password });

    token = loginRes.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id]);
    await ctx.app.close();
  });

  it('deve solicitar OTP por SMS e retornar devHint em ambiente de teste', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/verification/phone/request')
      .set('Authorization', `Bearer ${token}`)
      .expect(201);

    expect(res.body.ok).toBe(true);
    // Em ambiente sem Twilio, devHint expõe o código para uso nos testes
    expect(res.body).toHaveProperty('devHint');
    expect(typeof res.body.devHint).toBe('string');
    expect(res.body.devHint).toHaveLength(6);
  });

  it('deve rejeitar código de verificação inválido com 400', async () => {
    await request(ctx.app.getHttpServer())
      .post('/auth/verification/phone/confirm')
      .set('Authorization', `Bearer ${token}`)
      .send({ code: '000000' })
      .expect(400);
  });

  it('deve confirmar o telefone com o código correto e marcar phoneVerifiedAt no banco', async () => {
    // Solicita novo OTP para obter o devHint
    const reqRes = await request(ctx.app.getHttpServer())
      .post('/auth/verification/phone/request')
      .set('Authorization', `Bearer ${token}`);

    const code: string = reqRes.body.devHint;

    const res = await request(ctx.app.getHttpServer())
      .post('/auth/verification/phone/confirm')
      .set('Authorization', `Bearer ${token}`)
      .send({ code })
      .expect(201);

    expect(res.body.ok).toBe(true);

    const user = await ctx.prisma.user.findUnique({ where: { id: passenger.id } });
    expect(user!.phoneVerifiedAt).not.toBeNull();
  });

  it('ao solicitar OTP novamente com telefone já verificado deve retornar ok sem gerar novo código', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/verification/phone/request')
      .set('Authorization', `Bearer ${token}`)
      .expect(201);

    expect(res.body.ok).toBe(true);
    expect(res.body.message).toMatch(/verificado/i);
  });

  it('deve retornar 401 sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .post('/auth/verification/phone/request')
      .expect(401);
  });
});
