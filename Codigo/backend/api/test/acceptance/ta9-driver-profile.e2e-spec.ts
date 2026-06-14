/**
 * TA9 — Perfil e localização do motorista
 *
 * Verifica que o motorista consegue consultar seu perfil completo, atualizar
 * dados cadastrais (chave PIX, modelo do veículo) e enviar atualizações de
 * localização GPS que são persistidas no banco de dados.
 *
 * Documentação: Tabela 43 — Caso de teste de aceitação 9
 * Sistemas envolvidos: Mobile App (Motorista) ↔ MobU API
 * Interface: GET /driver/me; PATCH /driver/profile; PATCH /driver/location;
 *            GET /driver/available
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import {
  seedDriver,
  seedRegion,
  cleanupUsers,
  cleanupRegion,
  PARA_DE_MINAS,
  SeededDriver,
  SeededRegion,
} from '../helpers/seed';

describe('TA9 — Perfil e localização do motorista', () => {
  let ctx: TestApp;
  let driver: SeededDriver;
  let region: SeededRegion;
  let driverToken: string;

  beforeAll(async () => {
    ctx = await createTestApp();

    region = await seedRegion(ctx.prisma);
    driver = await seedDriver(ctx.prisma, '+5531920090101');

    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: driver.phone, password: driver.password });

    driverToken = loginRes.body.accessToken;
  });

  afterAll(async () => {
    await cleanupUsers(ctx.prisma, [driver.id]);
    await cleanupRegion(ctx.prisma, region.regionId);
    await ctx.app.close();
  });

  it('GET /driver/me deve retornar perfil completo do motorista autenticado', async () => {
    const res = await request(ctx.app.getHttpServer())
      .get('/driver/me')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(res.body.userId).toBe(driver.id);
    expect(res.body).toHaveProperty('approvalStatus');
    expect(res.body).toHaveProperty('cnhNumber');
    expect(res.body).toHaveProperty('vehicleModel');
    expect(res.body.approvalStatus).toBe('APPROVED');
  });

  it('PATCH /driver/profile deve atualizar dados do veículo e salvar no banco', async () => {
    const res = await request(ctx.app.getHttpServer())
      .patch('/driver/profile')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({
        vehicleModel: 'Onix',
        vehicleColor: 'Preto',
        vehicleYear: 2023,
        pixKey: driver.phone,
      })
      .expect(200);

    expect(res.body).toHaveProperty('vehicleModel');

    const profile = await ctx.prisma.driverProfile.findUnique({
      where: { userId: driver.id },
    });
    expect(profile!.vehicleModel).toBe('Onix');
    expect(profile!.vehicleColor).toBe('Preto');
  });

  it('PATCH /driver/location deve atualizar a posição GPS do motorista', async () => {
    const newLat = -19.875;
    const newLng = -44.620;

    await request(ctx.app.getHttpServer())
      .patch('/driver/location')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ lat: newLat, lng: newLng })
      .expect(200);

    const profile = await ctx.prisma.driverProfile.findUnique({
      where: { userId: driver.id },
    });
    expect(Number(profile!.currentLat)).toBeCloseTo(newLat, 4);
    expect(Number(profile!.currentLng)).toBeCloseTo(newLng, 4);
  });

  it('GET /driver/available deve listar o motorista quando online e disponível', async () => {
    // Garante que está online
    await request(ctx.app.getHttpServer())
      .post('/driver/status')
      .set('Authorization', `Bearer ${driverToken}`)
      .send({ online: true, available: true });

    const res = await request(ctx.app.getHttpServer())
      .get('/driver/available')
      .set('Authorization', `Bearer ${driverToken}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    const profileIds = res.body.map((d: { id: string }) => d.id);
    expect(profileIds).toContain(driver.profileId);
  });

  it('deve retornar 401 ao tentar acessar /driver/me sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .get('/driver/me')
      .expect(401);
  });

  it('deve retornar 401 ao tentar atualizar localização sem autenticação', async () => {
    await request(ctx.app.getHttpServer())
      .patch('/driver/location')
      .send({ lat: PARA_DE_MINAS.lat, lng: PARA_DE_MINAS.lng })
      .expect(401);
  });
});
