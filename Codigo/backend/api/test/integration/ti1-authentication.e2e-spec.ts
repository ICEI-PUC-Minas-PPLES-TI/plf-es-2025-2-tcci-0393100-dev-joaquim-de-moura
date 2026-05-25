/**
 * TI1 — Autenticação de usuário
 *
 * Verifica que credenciais válidas retornam HTTP 200 com JWT e dados do usuário,
 * e que o token permite acesso a rotas autenticadas (GET /auth/me).
 *
 * Documentação: Tabela 28 — Caso de teste de integração 1
 * Sistemas envolvidos: Mobile App (Passageiro/Motorista) ↔ MobU API
 * Interface: POST /auth/login → JWT; GET /auth/me → perfil do usuário
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedPassenger,
  seedDriver,
  cleanupUsers,
  SeededPassenger,
  SeededDriver,
} from '../helpers/seed';

describe('TI1 — Autenticação de usuário', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;

  beforeAll(async () => {
    ctx = await createTestApp();

    passenger = await seedPassenger(ctx.prisma, '+5531910010101');
    driver = await seedDriver(ctx.prisma, '+5531910010102');
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await ctx.app.close();
  });

  // ─── Cenário principal ────────────────────────────────────────────────────

  it('deve retornar 201, JWT e dados do usuário ao autenticar passageiro com credenciais válidas', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: passenger.password })
      .expect(201);

    expect(res.body).toHaveProperty('accessToken');
    expect(typeof res.body.accessToken).toBe('string');
    expect(res.body.accessToken.length).toBeGreaterThan(10);
  });

  it('deve retornar 201 e JWT ao autenticar motorista com credenciais válidas', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: driver.phone, password: driver.password })
      .expect(201);

    expect(res.body).toHaveProperty('accessToken');
  });

  it('deve permitir acesso a GET /auth/me com token JWT válido e retornar dados do passageiro', async () => {
    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: passenger.password });

    const token: string = loginRes.body.accessToken;

    const meRes = await request(ctx.app.getHttpServer())
      .get('/auth/me')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(meRes.body).toHaveProperty('id');
    expect(meRes.body.phone).toBe(passenger.phone);
    expect(meRes.body.role).toBe('PASSENGER');
  });

  it('deve permitir acesso a GET /auth/me com token JWT válido e retornar dados do motorista', async () => {
    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: driver.phone, password: driver.password });

    const token: string = loginRes.body.accessToken;

    const meRes = await request(ctx.app.getHttpServer())
      .get('/auth/me')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(meRes.body.phone).toBe(driver.phone);
    expect(meRes.body.role).toBe('DRIVER');
  });

  // ─── Cenários negativos ───────────────────────────────────────────────────

  it('deve retornar 401 para senha incorreta', async () => {
    await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: 'SenhaErrada@99' })
      .expect(401);
  });

  it('deve retornar 401 para usuário inexistente', async () => {
    await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: '+5531999999999', password: 'Senha@123' })
      .expect(401);
  });

  it('deve retornar 401 ao acessar GET /auth/me sem token', async () => {
    await request(ctx.app.getHttpServer()).get('/auth/me').expect(401);
  });
});
