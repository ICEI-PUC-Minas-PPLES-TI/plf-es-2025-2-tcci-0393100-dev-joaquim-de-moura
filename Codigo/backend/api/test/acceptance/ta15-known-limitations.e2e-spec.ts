/**
 * TA15 — Limitações conhecidas do sistema
 *
 * Esta suíte documenta funcionalidades que ainda NÃO foram implementadas
 * na versão atual da MobU API. Os testes são marcados com `it.failing()`
 * para que o runner os registre como "falha esperada" — eles passam no CI
 * exatamente porque o comportamento esperado ainda não existe.
 *
 * Quando a funcionalidade for implementada, remova o `.failing()` do teste
 * correspondente e ele voltará a ser um teste verde normal.
 *
 * Documentação: Tabela 49 — Caso de teste de aceitação 15
 * Sistemas envolvidos: Mobile App / Painel Admin ↔ MobU API
 *
 * ATENÇÃO — estes testes FALHAM intencionalmente.
 * Isso é esperado e documenta as limitações conhecidas do sistema.
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

describe('TA15 — Limitações conhecidas do sistema (falhas esperadas)', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let driver: SeededDriver;
  let region: SeededRegion;
  let passengerToken: string;
  let driverToken: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    passenger = await seedPassenger(ctx.prisma, '+5531920150101');
    driver = await seedDriver(ctx.prisma, '+5531920150102');

    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: passenger.phone, password: passenger.password });
    passengerToken = loginRes.body.accessToken;

    const driverLoginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: driver.phone, password: driver.password });
    driverToken = driverLoginRes.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  // ── LK-1: Rate limiting não implementado ─────────────────────────────────
  //
  // Comportamento esperado (quando implementado): após N tentativas rápidas
  // de login com credenciais erradas, a API deve retornar 429 Too Many Requests.
  //
  // Comportamento atual: não há limitação de taxa — todas as requisições
  // retornam 401 (credenciais inválidas), sem nenhum throttle.

  it.failing(
    'LK-1: deve retornar 429 após 10 tentativas de login com senha errada (rate limiting ausente)',
    async () => {
      const attempts = Array.from({ length: 10 }, () =>
        request(ctx.app.getHttpServer())
          .post('/auth/login')
          .send({ phone: passenger.phone, password: 'SenhaErrada@999' }),
      );

      const results = await Promise.all(attempts);
      const lastResult = results[results.length - 1];

      // Esperamos 429 — mas o sistema retorna 401 porque rate limiting não existe
      expect(lastResult.status).toBe(429);
    },
  );

  // ── LK-2: E-mail não verificado não bloqueia criação de corridas ──────────
  //
  // Comportamento esperado (quando implementado): um passageiro cujo e-mail
  // ainda não foi confirmado deve receber 403 ao tentar criar uma corrida.
  //
  // Comportamento atual: e-mail não verificado não é um pré-requisito para
  // solicitar corridas — o endpoint aceita a requisição normalmente (201).

  it.failing(
    'LK-2: deve retornar 403 ao criar corrida sem e-mail verificado (verificação de e-mail não exigida)',
    async () => {
      // O passageiro de teste não tem emailVerifiedAt preenchido
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
        });

      // Esperamos 403 — mas o sistema retorna 201 porque não exige e-mail verificado
      expect(res.status).toBe(403);
    },
  );

  // ── LK-3: Cabeçalhos de paginação ausentes em listagens ──────────────────
  //
  // Comportamento esperado (quando implementado): endpoints de listagem
  // (GET /rides, GET /admin/drivers, etc.) devem retornar o cabeçalho
  // X-Total-Count com o total de registros para permitir paginação no cliente.
  //
  // Comportamento atual: nenhum cabeçalho de paginação é retornado.

  it.failing(
    'LK-3: deve retornar cabeçalho X-Total-Count nas listagens (paginação ausente)',
    async () => {
      const res = await request(ctx.app.getHttpServer())
        .get('/rides')
        .set('Authorization', `Bearer ${passengerToken}`);

      // Esperamos o cabeçalho — mas ele não existe no sistema atual
      expect(res.headers).toHaveProperty('x-total-count');
    },
  );

  // ── LK-4: Localização do motorista não validada contra região de serviço ──
  //
  // Comportamento esperado (quando implementado): ao marcar-se como disponível
  // (online), o motorista deve estar dentro de uma região de operação ativa;
  // caso contrário a API deve retornar 403.
  //
  // Comportamento atual: qualquer coordenada é aceita ao ir online — o sistema
  // não verifica se a localização pertence a uma região cadastrada.

  it.failing(
    'LK-4: deve retornar 403 ao motorista ficar online fora de região ativa (validação de localização ausente)',
    async () => {
      // Coordenadas fora de qualquer região de operação cadastrada (oceano Atlântico)
      const res = await request(ctx.app.getHttpServer())
        .patch('/drivers/location')
        .set('Authorization', `Bearer ${driverToken}`)
        .send({
          lat: 0.0,
          lng: 0.0,
          goOnline: true,
        });

      // Esperamos 403 — mas o sistema aceita qualquer coordenada
      expect(res.status).toBe(403);
    },
  );

  // ── LK-5: Suporte a múltiplos idiomas (i18n) não implementado ────────────
  //
  // Comportamento esperado (quando implementado): o cabeçalho Accept-Language
  // deve ser respeitado e as mensagens de erro retornadas no idioma solicitado.
  //
  // Comportamento atual: todas as respostas de erro são sempre em português,
  // independentemente do cabeçalho enviado.

  it.failing(
    'LK-5: deve retornar mensagem de erro em inglês quando Accept-Language é en-US (i18n ausente)',
    async () => {
      const res = await request(ctx.app.getHttpServer())
        .post('/auth/login')
        .set('Accept-Language', 'en-US')
        .send({ phone: passenger.phone, password: 'SenhaErrada@999' });

      // Esperamos mensagem em inglês — mas o sistema retorna sempre em português
      const message: string =
        typeof res.body.message === 'string'
          ? res.body.message
          : JSON.stringify(res.body.message);

      expect(message.toLowerCase()).toMatch(/invalid|credentials|unauthorized/);
    },
  );
});
