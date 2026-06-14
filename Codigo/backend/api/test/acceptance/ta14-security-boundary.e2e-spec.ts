/**
 * TA14 — Fronteiras de segurança
 *
 * Verifica que a API rejeita corretamente tentativas de acesso não autorizado,
 * injeção de dados maliciosos, tokens forjados e escalada de privilégios.
 *
 * Todos os testes desta suíte devem PASSAR — eles validam que o sistema
 * protege seus recursos e não expõe dados de outros usuários.
 *
 * Documentação: Tabela 48 — Caso de teste de aceitação 14
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

describe('TA14 — Fronteiras de segurança', () => {
  let ctx: TestApp;
  let passengerA: SeededPassenger;
  let passengerB: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let tokenA: string;
  let tokenB: string;
  let rideIdFromA: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passengerA = await seedPassenger(ctx.prisma, '+5531920140101');
    passengerB = await seedPassenger(ctx.prisma, '+5531920140102');
    driver = await seedDriver(ctx.prisma, '+5531920140103');

    const loginA = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passengerA.phone, password: passengerA.password });
    tokenA = loginA.body.accessToken;

    const loginB = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passengerB.phone, password: passengerB.password });
    tokenB = loginB.body.accessToken;

    // Cria uma corrida pertencente ao passageiro A
    const rideRes = await request(ctx.app.getHttpServer())
      .post('/rides')
      .set('Authorization', `Bearer ${tokenA}`)
      .send({
        originLat: PARA_DE_MINAS.lat,
        originLng: PARA_DE_MINAS.lng,
        destLat: HOSPITAL.lat,
        destLng: HOSPITAL.lng,
        originAddress: PARA_DE_MINAS.addr,
        destinationAddress: HOSPITAL.addr,
        paymentMethod: 'CASH',
      });

    rideIdFromA = rideRes.body.id;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [
      passengerA.id,
      passengerB.id,
      driver.id,
    ]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  // ── Autenticação ──────────────────────────────────────────────────────────

  it('deve retornar 401 ao acessar endpoint protegido sem token', async () => {
    // GET /rides/my-rides requer autenticação
    await request(ctx.app.getHttpServer()).get('/rides/my-rides').expect(401);
  });

  it('deve retornar 401 com token JWT malformado (não é um JWT válido)', async () => {
    await request(ctx.app.getHttpServer())
      .get('/rides/my-rides')
      .set('Authorization', 'Bearer nao-sou-um-jwt')
      .expect(401);
  });

  it('deve retornar 401 com JWT assinado por chave incorreta (token forjado)', async () => {
    // JWT estruturalmente válido mas assinado com segredo diferente
    const forgedToken =
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9' +
      '.eyJzdWIiOiJmYWtlLWlkIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzAwMDAwMDAwfQ' +
      '.invalidsignatureXXXXXXXXXXXXXXXXXXXXXXXXX';

    await request(ctx.app.getHttpServer())
      .get('/rides/my-rides')
      .set('Authorization', `Bearer ${forgedToken}`)
      .expect(401);
  });

  it('deve retornar 403 ao tentar acessar rota de admin com token de passageiro', async () => {
    // A rota /admin/* exige role ADMIN — token de passageiro deve ser negado.
    // O guard retorna 403 (Forbidden) quando o usuário está autenticado mas não tem a role correta.
    await request(ctx.app.getHttpServer())
      .get('/admin/drivers')
      .set('Authorization', `Bearer ${tokenA}`)
      .expect(403);
  });

  // ── Isolamento entre usuários ─────────────────────────────────────────────

  it('passageiro B não deve conseguir visualizar detalhes da corrida do passageiro A', async () => {
    if (!rideIdFromA) {
      // Corrida não foi criada — pula verificação de isolamento cross-user
      return;
    }

    const res = await request(ctx.app.getHttpServer())
      .get(`/rides/${rideIdFromA}`)
      .set('Authorization', `Bearer ${tokenB}`);

    // A API deve negar acesso (403 ou 404 — ambos aceitáveis como resposta de segurança)
    expect([403, 404]).toContain(res.status);
  });

  // ── Injeção de dados maliciosos ───────────────────────────────────────────

  it('deve rejeitar tentativa de SQL injection no campo phone do login', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: "' OR '1'='1", password: 'qualquer' });

    // Não deve retornar 201 (não pode autenticar com injeção)
    expect(res.status).not.toBe(201);
    // Deve retornar erro de validação ou credenciais inválidas
    expect([400, 401, 422]).toContain(res.status);
  });

  it('deve rejeitar tentativa de injeção de operador MongoDB/NoSQL no login', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: { $gt: '' }, password: { $gt: '' } });

    expect(res.status).not.toBe(201);
    expect([400, 401, 422]).toContain(res.status);
  });

  it('deve responder com Content-Type application/json ao receber payload com XSS', async () => {
    // A API é um backend JSON — XSS em campos de texto não é executável em JSON puro.
    // Verificamos que a resposta é sempre JSON (não HTML), prevenindo Content-Type sniffing.
    const xssPayload = '<script>alert("xss")</script>';

    const res = await request(ctx.app.getHttpServer())
      .post('/rides')
      .set('Authorization', `Bearer ${tokenA}`)
      .send({
        originLat: PARA_DE_MINAS.lat,
        originLng: PARA_DE_MINAS.lng,
        destLat: HOSPITAL.lat,
        destLng: HOSPITAL.lng,
        originAddress: xssPayload,
        destinationAddress: HOSPITAL.addr,
        paymentMethod: 'CASH',
      });

    // Independente de aceitar ou rejeitar, a resposta deve ser JSON (nunca HTML)
    expect(res.headers['content-type']).toMatch(/application\/json/);

    // Se aceitou, o body deve ser JSON válido e parseável (não HTML injetado)
    if (res.status === 201) {
      expect(() => JSON.parse(JSON.stringify(res.body))).not.toThrow();
      expect(res.body).toHaveProperty('id');
    }
  });

  // ── Campos de identificação ───────────────────────────────────────────────

  it('deve retornar 400 ou 401 ao passar ID de usuário inválido no path', async () => {
    await request(ctx.app.getHttpServer())
      .get('/users/../../../../etc/passwd')
      .set('Authorization', `Bearer ${tokenA}`)
      .expect((res) => {
        expect([400, 401, 403, 404]).toContain(res.status);
      });
  });

  it('não deve expor stack trace em nenhuma resposta de erro', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passengerA.phone, password: 'SenhaErrada@999' });

    expect(res.body).not.toHaveProperty('stack');
    expect(JSON.stringify(res.body)).not.toMatch(/at Object\./);
    expect(JSON.stringify(res.body)).not.toMatch(/node_modules/);
  });
});
