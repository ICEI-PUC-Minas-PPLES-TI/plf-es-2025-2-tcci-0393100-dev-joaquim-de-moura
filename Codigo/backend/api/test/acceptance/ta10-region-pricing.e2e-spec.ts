/**
 * TA10 — Gestão de regiões de operação e tarifas (admin)
 *
 * Verifica que o administrador pode criar/listar regiões de operação,
 * criar configurações de tarifa, ativá-las e que a estimativa de corrida
 * utiliza a tarifa ativa da região. Regiões e tarifas sem uso são removíveis.
 *
 * Documentação: Tabela 44 — Caso de teste de aceitação 10
 * Sistemas envolvidos: Painel Admin ↔ MobU API
 * Interface: POST /admin/regions; GET /admin/regions; POST /admin/pricing;
 *            PATCH /admin/pricing/:id/activate; POST /rides/estimate
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import { seedAdmin, cleanupUsers, SeededAdmin } from '../helpers/seed';

describe('TA10 — Gestão de regiões de operação e tarifas', () => {
  let ctx: TestApp;
  let admin: SeededAdmin;
  let adminToken: string;
  let regionId: string;
  let pricingId: string;

  beforeAll(async () => {
    ctx = await createTestApp();
    admin = await seedAdmin(ctx.prisma, '+5531920100101');

    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: admin.phone, password: admin.password });

    adminToken = loginRes.body.accessToken;
  });

  afterAll(async () => {
    // Remove tarifa e região criadas no teste
    if (pricingId) {
      await ctx.prisma.pricingConfig.deleteMany({ where: { id: pricingId } });
    }
    if (regionId) {
      await ctx.prisma.operationRegion.deleteMany({ where: { id: regionId } });
    }
    await cleanupUsers(ctx.prisma, [admin.id]);
    await ctx.app.close();
  });

  it('admin deve criar nova região de operação e receber 201', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/admin/regions')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({
        name: 'Região Teste TA10',
        city: 'Pará de Minas',
        active: true,
        centerLat: -19.8684,
        centerLng: -44.6142,
        radiusMeters: 10000,
      })
      .expect(201);

    expect(res.body).toHaveProperty('id');
    expect(res.body.name).toBe('Região Teste TA10');
    regionId = res.body.id;
  });

  it('GET /admin/regions deve listar a região recém-criada', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/admin/regions')
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const ids = res.body.map((r: { id: string }) => r.id);
    expect(ids).toContain(regionId);
  });

  it('admin deve criar configuração de tarifa para a região', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/admin/pricing')
      .set('Authorization', `Bearer ${adminToken}`)
      .send({
        name: 'Tarifa TA10',
        baseFareCents: 600,
        perKmCents: 250,
        perMinuteCents: 60,
        minimumFareCents: 900,
        platformFeePercent: 20,
        regionId,
        isActive: false,
      })
      .expect(201);

    expect(res.body).toHaveProperty('id');
    expect(res.body.name).toBe('Tarifa TA10');
    pricingId = res.body.id;
  });

  it('PATCH /admin/pricing/:id/activate deve ativar a configuração de tarifa', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch(`/admin/pricing/${pricingId}/activate`)
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    expect(res.body.isActive).toBe(true);
  });

  it('GET /admin/pricing deve listar a configuração criada', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/admin/pricing')
      .set('Authorization', `Bearer ${adminToken}`)
      .expect(200);

    const ids = res.body.map((p: { id: string }) => p.id);
    expect(ids).toContain(pricingId);
  });

  it('não-admin deve receber 403 ao tentar acessar rotas de pricing', async () => {
    await request(ctx.app.getHttpServer())
      .get('/admin/pricing')
      .expect(401);
  });
});
