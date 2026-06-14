/**
 * TA13 — Resiliência do sistema
 *
 * Verifica que a API responde de forma controlada a situações adversas:
 * banco de dados indisponível, payloads malformados, campos ausentes,
 * requisições simultâneas e rotas inexistentes.
 *
 * Todos os testes desta suíte devem PASSAR — eles validam que o sistema
 * trata erros de forma segura e previsível, nunca expondo stack traces
 * ou falhando silenciosamente.
 *
 * Documentação: Tabela 47 — Caso de teste de aceitação 13
 * Sistemas envolvidos: Mobile App / Painel Admin ↔ MobU API
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
import { AuthService } from '../../src/auth/auth.service';

describe('TA13 — Resiliência do sistema', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let passengerToken: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531920130101');
    driver = await seedDriver(ctx.prisma, '+5531920130102');

    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: passenger.password });

    passengerToken = loginRes.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  // ── Banco de dados ────────────────────────────────────────────────────────

  it('deve retornar 500 quando o banco de dados está indisponível', async () => {
    // Simula falha de conexão com o banco interceptando o método de login do AuthService.
    // A abordagem via serviço é necessária pois Prisma v6 usa Proxy internamente e
    // jest.spyOn sobre o delegate não intercepta as chamadas de forma confiável.
    const authService = ctx.app.get(AuthService);
    const spy = jest
      .spyOn(authService, 'login')
      .mockRejectedValueOnce(
        new Error('Connection refused: database is not available'),
      );

    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: passenger.password });

    spy.mockRestore();

    // O sistema deve retornar 500 e não vazar detalhes internos
    expect(res.status).toBe(500);
    expect(res.body).not.toHaveProperty('stack');
  });

  // ── Payloads inválidos ────────────────────────────────────────────────────

  it('deve retornar 400 ao receber JSON malformado', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .set('Content-Type', 'application/json')
      .send('{ phone: "invalido", }'); // JSON inválido (chave sem aspas + trailing comma)

    expect([400, 422]).toContain(res.status);
  });

  it('deve retornar 400 ao registrar passageiro sem campos obrigatórios', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/register')
      .send({
        name: 'Incompleto',
        // phone ausente
        password: 'Senha@123',
      })
      .expect(400);

    expect(res.body).toHaveProperty('message');
  });

  it('deve retornar 400 ao criar corrida sem coordenadas de destino', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/rides')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({
        originLat: PARA_DE_MINAS.lat,
        originLng: PARA_DE_MINAS.lng,
        originAddress: PARA_DE_MINAS.addr,
        // destLat e destLng ausentes
        paymentMethod: 'CASH',
      })
      .expect(400);

    expect(res.body).toHaveProperty('message');
  });

  // ── Rotas inexistentes ────────────────────────────────────────────────────

  it('deve retornar 404 para rota que não existe', async () => {
    await request(ctx.app.getHttpServer())
      .get('/rota-que-nao-existe-nunca')
      .expect(404);
  });

  it('deve retornar 404 para recurso com ID inexistente', async () => {
    await request(ctx.app.getHttpServer())
      .get('/rides/id-que-nao-existe-no-banco')
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(404);
  });

  // ── Requisições simultâneas ───────────────────────────────────────────────

  it('deve processar 10 requisições de login simultâneas sem corrupção', async () => {
    const requests = Array.from({ length: 10 }, () =>
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
    );

    const results = await Promise.all(requests);

    // Todas devem retornar 201 com token válido
    for (const res of results) {
      expect(res.status).toBe(201);
      expect(res.body).toHaveProperty('accessToken');
    }
  });

  it('deve processar 5 solicitações de corrida simultâneas sem duplicar registros', async () => {
    // Cria 5 corridas simultâneas com o mesmo passageiro
    const requests = Array.from({ length: 5 }, (_, i) =>
      request(ctx.app.getHttpServer())
        .post('/rides')
        .set('Authorization', `Bearer ${passengerToken}`)
        .send({
          originLat: PARA_DE_MINAS.lat + i * 0.001,
          originLng: PARA_DE_MINAS.lng,
          destLat: HOSPITAL.lat,
          destLng: HOSPITAL.lng,
          originAddress: PARA_DE_MINAS.addr,
          destinationAddress: HOSPITAL.addr,
          paymentMethod: 'CASH',
        }),
    );

    const results = await Promise.all(requests);

    // Todas devem retornar 201 (o sistema aceita várias corridas pendentes)
    const statuses = results.map((r) => r.status);
    expect(statuses.every((s) => s === 201)).toBe(true);

    // IDs devem ser únicos (sem duplicação)
    const ids = results.map((r) => r.body.id).filter(Boolean);
    const uniqueIds = new Set(ids);
    expect(uniqueIds.size).toBe(ids.length);
  });
});
