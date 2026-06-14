/**
 * TA8 — Aprovação e rejeição de motorista pelo administrador
 *
 * Verifica que o administrador consegue listar motoristas pendentes,
 * aprovar um motorista (permitindo que ele aceite corridas) e rejeitar
 * outro com motivo, além de bloquear/desbloquear usuários. Não-admins
 * devem ser bloqueados nas rotas administrativas.
 *
 * Documentação: Tabela 42 — Caso de teste de aceitação 8
 * Sistemas envolvidos: Painel Admin ↔ MobU API
 * Interface: GET /admin/drivers/pending; PATCH /admin/drivers/:id/approve;
 *            PATCH /admin/drivers/:id/reject; PATCH /admin/users/:id/block
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedPendingDriver,
  seedAdmin,
  seedPassenger,
  cleanupUsers,
  SeededDriver,
  SeededAdmin,
  SeededPassenger,
} from '../helpers/seed';

describe('TA8 — Aprovação e rejeição de motorista pelo administrador', () => {
  let ctx: TestApp;
  let pendingDriver1: SeededDriver;
  let pendingDriver2: SeededDriver;
  let admin: SeededAdmin;
  let passenger: SeededPassenger;
  let adminToken: string;
  let passengerToken: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    pendingDriver1 = await seedPendingDriver(ctx.prisma, '+5531920080101');
    pendingDriver2 = await seedPendingDriver(ctx.prisma, '+5531920080102');
    admin = await seedAdmin(ctx.prisma, '+5531920080103');
    passenger = await seedPassenger(ctx.prisma, '+5531920080104');

    const [al, pl] = await Promise.all([
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: admin.phone, password: admin.password }),
      request(ctx.app.getHttpServer())
        .post('/auth/login')
        .send({ phone: passenger.phone, password: passenger.password }),
    ]);

    adminToken = al.body.accessToken;
    passengerToken = pl.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [
      pendingDriver1.id,
      pendingDriver2.id,
      admin.id,
      passenger.id,
    ]);
    await ctx.app.close();
  });

  it('passageiro não deve ter acesso às rotas de admin (403)', async () => {
    await request(ctx.app.getHttpServer())
      .get('/admin/drivers/pending')
      .set('Authorization', `Bearer ${passengerToken}`)
      .expect(403);
  });

  it('GET /admin/drivers/pending deve listar os motoristas aguardando aprovação', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/admin/drivers/pending')
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const userIds = res.body.map((d: { userId: string }) => d.userId);
    expect(userIds).toContain(pendingDriver1.id);
    expect(userIds).toContain(pendingDriver2.id);
  });

  it('admin deve aprovar motorista 1 e mudar approvalStatus para APPROVED', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/admin/drivers/${pendingDriver1.profileId}/approve`)
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(res.body.approvalStatus).toBe('APPROVED');

    const profile = await ctx.prisma.driverProfile.findUnique({
      where: { userId: pendingDriver1.id },
    });
    expect(profile!.approvalStatus).toBe('APPROVED');
    expect(profile!.approvedAt).not.toBeNull();
  });

  it('admin deve rejeitar motorista 2 com motivo e mudar approvalStatus para REJECTED', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/admin/drivers/${pendingDriver2.profileId}/reject`)
      .set('Authorization', `Bearer ${adminToken}`)
      .send({ reason: 'Documentação incompleta — CNH vencida.' })
      .expect(200);

    expect(res.body.approvalStatus).toBe('REJECTED');

    const profile = await ctx.prisma.driverProfile.findUnique({
      where: { userId: pendingDriver2.id },
    });
    expect(profile!.approvalStatus).toBe('REJECTED');
    expect(profile!.rejectionReason).toMatch(/CNH/i);
  });

  it('GET /admin/drivers/approved deve conter motorista 1 aprovado', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/admin/drivers/approved')
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    const userIds = res.body.map((d: { userId: string }) => d.userId);
    expect(userIds).toContain(pendingDriver1.id);
  });

  it('admin deve bloquear passageiro e confirmar no banco', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/admin/users/${passenger.id}/block`)
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(res.body).toHaveProperty('blocked');

    const user = await ctx.prisma.user.findUnique({ where: { id: passenger.id } });
    expect(user!.blocked).toBe(true);
  });

  it('admin deve desbloquear o passageiro', async () => {
    await request(ctx.app.getHttpServer())
      .patch(`/admin/users/${passenger.id}/unblock`)
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    const user = await ctx.prisma.user.findUnique({ where: { id: passenger.id } });
    expect(user!.blocked).toBe(false);
  });

  it('deve retornar 401 sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .get('/admin/drivers/pending')
      .expect(401);
  });
});
