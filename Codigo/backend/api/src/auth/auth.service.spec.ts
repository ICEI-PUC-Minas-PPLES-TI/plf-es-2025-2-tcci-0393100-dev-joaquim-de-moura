import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException, ForbiddenException, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';
import { UserRole } from '@prisma/client';
import { AuthService } from './auth.service';
import { PrismaService } from '../prisma/prisma.service';
import { VerificationService } from './verification.service';

const HASHED_PASSWORD = bcrypt.hashSync('senha123', 10);

const USER_PASSENGER = {
  id: 'user-1',
  phone: '31999990001',
  name: 'Mariana Silva',
  email: 'mariana@email.com',
  role: UserRole.PASSENGER,
  passwordHash: HASHED_PASSWORD,
  blocked: false,
  profilePhotoUrl: null,
  createdAt: new Date(),
};

describe('AuthService', () => {
  let service: AuthService;
  let prisma: jest.Mocked<PrismaService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuthService,
        {
          provide: PrismaService,
          useValue: {
            user: {
              findUnique: jest.fn(),
              findFirst: jest.fn(),
              create: jest.fn(),
              update: jest.fn(),
            },
            passengerProfile: {
              create: jest.fn(),
              findUnique: jest.fn(),
              deleteMany: jest.fn(),
            },
            driverProfile: {
              findUnique: jest.fn(),
              create: jest.fn(),
            },
            $transaction: jest.fn(),
          },
        },
        {
          provide: JwtService,
          useValue: { sign: jest.fn().mockReturnValue('fake-jwt-token') },
        },
        {
          provide: ConfigService,
          useValue: { get: jest.fn() },
        },
        {
          provide: VerificationService,
          useValue: {
            requestEmailOtp: jest.fn().mockResolvedValue(undefined),
          },
        },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    prisma = module.get(PrismaService);
  });

  it('deve ser instanciado corretamente', () => {
    expect(service).toBeDefined();
  });

  // ──────────────────────────────────────────────
  // login — TI1: autenticação de usuário
  // ──────────────────────────────────────────────
  describe('login (TI1)', () => {
    it('deve retornar accessToken e dados do usuário com credenciais válidas', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValue(USER_PASSENGER);

      const result = await service.login({ phone: '31999990001', password: 'senha123' });

      expect(result.accessToken).toBe('fake-jwt-token');
      expect(result.user.id).toBe('user-1');
      expect(result.user.role).toBe(UserRole.PASSENGER);
    });

    it('deve lançar UnauthorizedException para telefone não cadastrado', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValue(null);

      await expect(
        service.login({ phone: '99999999999', password: 'senha123' }),
      ).rejects.toThrow(UnauthorizedException);
    });

    it('deve lançar UnauthorizedException para senha incorreta', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValue(USER_PASSENGER);

      await expect(
        service.login({ phone: '31999990001', password: 'senhaErrada' }),
      ).rejects.toThrow(UnauthorizedException);
    });

    it('deve lançar ForbiddenException para usuário bloqueado (TA11)', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValue({
        ...USER_PASSENGER,
        blocked: true,
      });

      await expect(
        service.login({ phone: '31999990001', password: 'senha123' }),
      ).rejects.toThrow(ForbiddenException);
    });
  });

  // ──────────────────────────────────────────────
  // register — TA1: passageiro criar conta
  // ──────────────────────────────────────────────
  describe('register (TA1)', () => {
    const validRegisterDto = {
      phone: '31999990001',
      password: 'senha123',
      name: 'Mariana Silva',
      email: 'mariana@email.com',
      cpf: '529.982.247-25',
      birthDate: '1997-05-15',
      acceptedTerms: true,
      acceptedPrivacy: true,
    };

    beforeEach(() => {
      (prisma.user.findFirst as jest.Mock).mockResolvedValue(null);
      (prisma.passengerProfile.findUnique as jest.Mock).mockResolvedValue(null);
      (prisma.$transaction as jest.Mock).mockImplementation(async (fn) => {
        const txMock = {
          user: { create: jest.fn().mockResolvedValue(USER_PASSENGER) },
          passengerProfile: { create: jest.fn().mockResolvedValue({}) },
        };
        return fn(txMock);
      });
    });

    it('deve cadastrar passageiro e retornar accessToken (TA1)', async () => {
      const result = await service.register(validRegisterDto);

      expect(result.accessToken).toBe('fake-jwt-token');
      expect(result.user.role).toBe(UserRole.PASSENGER);
    });

    it('deve lançar BadRequestException para telefone já cadastrado', async () => {
      (prisma.user.findFirst as jest.Mock).mockResolvedValue({ ...USER_PASSENGER, phone: '31999990001' });

      await expect(service.register(validRegisterDto)).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException para e-mail já cadastrado', async () => {
      (prisma.user.findFirst as jest.Mock).mockResolvedValue({
        ...USER_PASSENGER,
        phone: 'outro-telefone',
        email: 'mariana@email.com',
      });

      await expect(service.register(validRegisterDto)).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException sem aceitar os termos', async () => {
      await expect(
        service.register({ ...validRegisterDto, acceptedTerms: false }),
      ).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException para CPF inválido', async () => {
      await expect(
        service.register({ ...validRegisterDto, cpf: '000.000.000-00' }),
      ).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException para telefone com menos de 10 dígitos', async () => {
      await expect(
        service.register({ ...validRegisterDto, phone: '123' }),
      ).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException para nome muito curto', async () => {
      await expect(
        service.register({ ...validRegisterDto, name: 'AB' }),
      ).rejects.toThrow(BadRequestException);
    });
  });
});
