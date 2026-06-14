/**
 * TA1 — Cadastro de passageiro
 *
 * Verifica o fluxo completo de cadastro de um novo passageiro: submissão
 * dos dados, recebimento do JWT, acesso autenticado ao perfil e detecção
 * de duplicidade (telefone e e-mail já cadastrados).
 *
 * Documentação: Tabela 35 — Caso de teste de aceitação 1
 * Sistemas envolvidos: Mobile App (Passageiro) ↔ MobU API
 * Interface: POST /auth/register → JWT; GET /auth/me → perfil
 */

import request from 'supertest';
import { createTestApp, TestApp } from '../helpers/create-test-app';
import { cleanupUsers } from '../helpers/seed';

describe('TA1 — Cadastro de passageiro', () => {
  let ctx: TestApp;
  const phone = '+5531920010101';
  const email = 'ta1.passageiro@mobu.test';
  let createdUserId: string;

  beforeAll(async () => {
    ctx = await createTestApp();
  });

  afterAll(async () => {
    if (createdUserId) {
      await cleanupUsers(ctx.prisma, [createdUserId]);
    }
    await ctx.app.close();
  });

  it('deve criar conta de passageiro e retornar 201 com accessToken', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/register')
      .send({
        name: 'Passageiro Aceitação',
        phone,
        email,
        password: 'Senha@123',
        cpf: '52998224725',
        birthDate: '1995-06-15',
        acceptedTerms: true,
        acceptedPrivacy: true,
      })
      .expect(201);

    expect(res.body).toHaveProperty('accessToken');
    expect(typeof res.body.accessToken).toBe('string');
    expect(res.body.user).toHaveProperty('id');
    expect(res.body.user.role).toBe('PASSENGER');

    createdUserId = res.body.user.id;
  });

  it('deve permitir login com as credenciais recém-cadastradas', async () => {
    const res = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: phone.replace(/\D/g, ''), password: 'Senha@123' })
      .expect(201);

    expect(res.body).toHaveProperty('accessToken');
  });

  it('deve retornar perfil correto em GET /auth/me com o token do registro', async () => {
    const loginRes = await request(ctx.app.getHttpServer())
      .post('/auth/login')
      .send({ phone: phone.replace(/\D/g, ''), password: 'Senha@123' });

    const token: string = loginRes.body.accessToken;

    const meRes = await request(ctx.app.getHttpServer())
      .get('/auth/me')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(meRes.body.phone).toBe(phone.replace(/\D/g, ''));
    expect(meRes.body.role).toBe('PASSENGER');
    expect(meRes.body.email).toBe(email);
  });

  it('deve retornar 400 ao tentar cadastrar com o mesmo telefone', async () => {
    await request(ctx.app.getHttpServer())
      .post('/auth/register')
      .send({
        name: 'Outro Passageiro',
        phone,
        email: 'outro@mobu.test',
        password: 'Senha@123',
        cpf: '11144477735',
        birthDate: '1990-01-01',
        acceptedTerms: true,
        acceptedPrivacy: true,
      })
      .expect(400);
  });

  it('deve retornar 400 ao tentar cadastrar com o mesmo e-mail', async () => {
    await request(ctx.app.getHttpServer())
      .post('/auth/register')
      .send({
        name: 'Outro Passageiro',
        phone: '+5531920010199',
        email,
        password: 'Senha@123',
        cpf: '11144477735',
        birthDate: '1990-01-01',
        acceptedTerms: true,
        acceptedPrivacy: true,
      })
      .expect(400);
  });

  it('deve retornar 400 sem aceitar os termos de uso', async () => {
    await request(ctx.app.getHttpServer())
      .post('/auth/register')
      .send({
        name: 'Passageiro Sem Termos',
        phone: '+5531920010198',
        email: 'semtermos@mobu.test',
        password: 'Senha@123',
        cpf: '11144477735',
        birthDate: '1990-01-01',
        acceptedTerms: false,
        acceptedPrivacy: true,
      })
      .expect(400);
  });
});
