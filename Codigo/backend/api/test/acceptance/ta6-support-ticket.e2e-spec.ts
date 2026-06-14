/**
 * TA6 — Abertura e gestão de chamado de suporte
 *
 * Verifica que o usuário pode abrir um chamado de suporte, consultá-lo
 * em "meus chamados" e que o administrador consegue listar todos os
 * chamados e atualizar o status para RESOLVED.
 *
 * Documentação: Tabela 40 — Caso de teste de aceitação 6
 * Sistemas envolvidos: Mobile App ↔ MobU API; Painel Admin ↔ MobU API
 * Interface: POST /support/tickets; GET /support/tickets/my;
 *            GET /support/tickets (admin); PATCH /support/tickets/:id (admin)
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedPassenger,
  seedAdmin,
  cleanupUsers,
  SeededPassenger,
  SeededAdmin,
} from '../helpers/seed';

describe('TA6 — Abertura e gestão de chamado de suporte', () => {
  let ctx: TestApp;
  let passenger: SeededPassenger;
  let admin: SeededAdmin;
  let passengerToken: string;
  let adminToken: string;
  let ticketId: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    passenger = await seedPassenger(ctx.prisma, '+5531920060101');
    admin = await seedAdmin(ctx.prisma, '+5531920060102');

    const [pl, al] = await Promise.all([
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: admin.phone, password: admin.password }),
    ]);

    passengerToken = pl.body.accessToken;
    adminToken = al.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [passenger.id, admin.id]);
    await ctx.app.close();
  });

  it('passageiro deve abrir chamado de suporte e receber 201 com id do ticket', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/support/tickets')
      .set('Authorization', `Bearer ${passengerToken}`)
      .send({
        subject: 'Problema com pagamento',
        description: 'O motorista não confirmou meu pagamento em dinheiro após a corrida.',
        type: 'PAYMENT',
      })
      .expect(201);

    expect(res.body).toHaveProperty('id');
    expect(res.body.status).toBe('OPEN');
    expect(res.body.subject).toBe('Problema com pagamento');
    expect(res.body.creatorId).toBe(passenger.id);

    ticketId = res.body.id;
  });

  it('GET /support/tickets/my deve retornar os chamados do passageiro', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/support/tickets/my')
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const ids = res.body.map((t: { id: string }) => t.id);
    expect(ids).toContain(ticketId);
  });

  it('admin deve listar todos os chamados via GET /support/tickets', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/support/tickets')
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const ids = res.body.map((t: { id: string }) => t.id);
    expect(ids).toContain(ticketId);
  });

  it('passageiro não deve ter acesso à listagem geral de chamados', async () => {
    await request(ctx.app.getHttpServer())
      .get('/support/tickets')
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(403);
  });

  it('admin deve atualizar chamado para RESOLVED com resolução', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/support/tickets/${ticketId}`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ status: 'RESOLVED', resolution: 'Pagamento confirmado manualmente pelo suporte.' })
      .expect(200);

    expect(res.body.status).toBe('RESOLVED');
    expect(res.body.resolution).toMatch(/confirmado/i);
    expect(res.body.closedAt).not.toBeNull();
  });

  it('deve retornar 401 sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .post('/support/tickets')
      .send({ subject: 'Teste', description: 'Descrição do problema aqui' })
      .expect(401);
  });
});
