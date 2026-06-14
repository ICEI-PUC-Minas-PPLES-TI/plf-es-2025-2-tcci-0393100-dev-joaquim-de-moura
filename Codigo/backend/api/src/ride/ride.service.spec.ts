import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException, ForbiddenException, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { PaymentStatus, RideStatus } from '@prisma/client';
import { GoogleDirectionsService } from '../maps/google-directions.service';
import { PrismaService } from '../prisma/prisma.service';
import { RealtimeService } from '../realtime/realtime.service';
import { NotificationService } from '../notification/notification.service';
import { RideService } from './ride.service';

const PRICING = {
  id: 'pricing-1',
  isActive: true,
  baseFareCents: 500,
  perKmCents: 200,
  perMinuteCents: 50,
  minimumFareCents: 800,
  bookingFeeCents: 0,
  surgeMultiplier: 1,
  currency: 'BRL',
  createdAt: new Date(),
};

const RIDE = {
  id: 'ride-1',
  passengerId: 'passenger-1',
  driverId: null,
  status: RideStatus.PENDING_DRIVER,
  originLat: -19.7,
  originLng: -44.8,
  destLat: -19.75,
  destLng: -44.85,
  originAddress: 'Rua A, 100',
  destinationAddress: 'Rua B, 200',
  distanceMeters: 5000,
  durationSeconds: 600,
  estimatedFareCents: 1800,
  paymentMethod: 'CASH',
  paymentStatus: PaymentStatus.PENDING,
  paymentPixPayload: null,
  paymentTxId: null,
  paymentConfirmedAt: null,
  paymentReportedAt: null,
  paymentConfirmedBy: null,
  pricingConfigId: 'pricing-1',
  createdAt: new Date(),
};

describe('RideService', () => {
  let service: RideService;
  let prisma: jest.Mocked<PrismaService>;
  let directions: jest.Mocked<GoogleDirectionsService>;
  let realtime: jest.Mocked<RealtimeService>;
  let config: jest.Mocked<ConfigService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RideService,
        {
          provide: PrismaService,
          useValue: {
            ride: {
              create: jest.fn(),
              findMany: jest.fn(),
              findFirst: jest.fn(),
              findUnique: jest.fn(),
              update: jest.fn(),
              aggregate: jest.fn(),
            },
            driverProfile: { findMany: jest.fn() },
            pricingConfig: { findFirst: jest.fn() },
            operationRegion: { findMany: jest.fn() },
            promoCode: { findFirst: jest.fn(), update: jest.fn() },
            review: { aggregate: jest.fn() },
          },
        },
        {
          provide: ConfigService,
          useValue: { get: jest.fn().mockReturnValue('FAKE_GOOGLE_KEY') },
        },
        {
          provide: GoogleDirectionsService,
          useValue: {
            getRoute: jest.fn().mockResolvedValue({
              distanceMeters: 5000,
              durationSeconds: 600,
              encodedPolyline: 'abc',
            }),
          },
        },
        {
          provide: RealtimeService,
          useValue: {
            emitToDrivers: jest.fn(),
            emitAdmin: jest.fn(),
            emitRide: jest.fn(),
            emitToUser: jest.fn(),
          },
        },
        {
          provide: NotificationService,
          useValue: {
            sendToUser: jest.fn(),
            sendToUsers: jest.fn(),
          },
        },
      ],
    }).compile();

    service = module.get<RideService>(RideService);
    prisma = module.get(PrismaService);
    directions = module.get(GoogleDirectionsService);
    realtime = module.get(RealtimeService);
    config = module.get(ConfigService);
  });

  it('deve ser instanciado corretamente', () => {
    expect(service).toBeDefined();
  });

  // ──────────────────────────────────────────────
  // estimate — TA2: passageiro solicita estimativa
  // ──────────────────────────────────────────────
  describe('estimate', () => {
    beforeEach(() => {
      (prisma.operationRegion.findMany as jest.Mock).mockResolvedValue([]);
      (prisma.pricingConfig.findFirst as jest.Mock).mockResolvedValue(PRICING);
    });

    it('deve retornar estimativa com distância, duração e valor (TA2)', async () => {
      const result = await service.estimate({
        originLat: -19.7,
        originLng: -44.8,
        destLat: -19.75,
        destLng: -44.85,
      });

      expect(result.distanceMeters).toBe(5000);
      expect(result.durationSeconds).toBe(600);
      expect(result.estimatedFareCents).toBeGreaterThanOrEqual(PRICING.minimumFareCents);
      expect(result.currency).toBe('BRL');
    });

    it('deve lançar BadRequestException se PricingConfig não existir', async () => {
      (prisma.pricingConfig.findFirst as jest.Mock).mockResolvedValue(null);

      await expect(
        service.estimate({ originLat: -19.7, originLng: -44.8, destLat: -19.75, destLng: -44.85 }),
      ).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException para coordenadas inválidas', async () => {
      await expect(
        service.estimate({ originLat: NaN, originLng: -44.8, destLat: -19.75, destLng: -44.85 }),
      ).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException se origem estiver fora da região de operação', async () => {
      (prisma.operationRegion.findMany as jest.Mock).mockResolvedValue([
        { id: 'region-1', active: true, centerLat: -23.5, centerLng: -46.6, radiusMeters: 1000 },
      ]);

      await expect(
        service.estimate({ originLat: -19.7, originLng: -44.8, destLat: -19.75, destLng: -44.85 }),
      ).rejects.toThrow(BadRequestException);
    });
  });

  // ──────────────────────────────────────────────
  // create — TA2: passageiro cria corrida
  // ──────────────────────────────────────────────
  describe('create', () => {
    beforeEach(() => {
      (prisma.operationRegion.findMany as jest.Mock).mockResolvedValue([]);
      (prisma.pricingConfig.findFirst as jest.Mock).mockResolvedValue(PRICING);
      (prisma.ride.create as jest.Mock).mockResolvedValue(RIDE);
      (prisma.driverProfile.findMany as jest.Mock).mockResolvedValue([]);
      (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue(null);
    });

    it('deve criar corrida e retornar o objeto (TA2)', async () => {
      const result = await service.create({
        passengerId: 'passenger-1',
        originLat: -19.7,
        originLng: -44.8,
        destLat: -19.75,
        destLng: -44.85,
      });

      expect(result.id).toBe('ride-1');
      expect(result.status).toBe(RideStatus.PENDING_DRIVER);
      expect(prisma.ride.create).toHaveBeenCalled();
    });

    it('deve emitir evento para motoristas próximos quando disponíveis', async () => {
      (prisma.driverProfile.findMany as jest.Mock).mockResolvedValue([
        { userId: 'driver-1', currentLat: -19.7, currentLng: -44.8 },
      ]);

      await service.create({
        passengerId: 'passenger-1',
        originLat: -19.7,
        originLng: -44.8,
        destLat: -19.75,
        destLng: -44.85,
      });

      expect(realtime.emitToDrivers).toHaveBeenCalledWith(
        ['driver-1'],
        'ride_offer',
        expect.any(Object),
      );
    });

    it('deve lançar BadRequestException sem API key do Google Maps', async () => {
      (config.get as jest.Mock).mockReturnValue(undefined);

      await expect(
        service.create({
          passengerId: 'passenger-1',
          originLat: -19.7,
          originLng: -44.8,
          destLat: -19.75,
          destLng: -44.85,
        }),
      ).rejects.toThrow(BadRequestException);
    });

    it('deve aplicar desconto percentual de cupom válido e incrementar usedCount', async () => {
      const promo = {
        id: 'promo-1',
        code: 'DESCONTO10',
        active: true,
        maxUses: null,
        usedCount: 0,
        expiresAt: null,
        discountPercent: 10,
        discountCents: null,
      };
      (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue(promo);
      (prisma.promoCode.update as jest.Mock).mockResolvedValue(promo);
      // fare calculado pelo service: baseFare(500) + 5km*200 + 10min*50 = 2000; 10% off = 1800
      (prisma.ride.create as jest.Mock).mockImplementation((args) =>
        Promise.resolve({ ...RIDE, estimatedFareCents: args.data.estimatedFareCents }),
      );

      const result = await service.create({
        passengerId: 'passenger-1',
        originLat: -19.7,
        originLng: -44.8,
        destLat: -19.75,
        destLng: -44.85,
        promoCode: 'DESCONTO10',
      });

      // 2000 cents - 10% = 1800 cents
      expect(result.estimatedFareCents).toBe(1800);
      expect(prisma.promoCode.update).toHaveBeenCalledWith(
        expect.objectContaining({ data: { usedCount: { increment: 1 } } }),
      );
    });
  });

  // ──────────────────────────────────────────────
  // updateStatus — passageiro cancela (TA2/TA5)
  // ──────────────────────────────────────────────
  describe('updateStatus', () => {
    it('deve permitir passageiro cancelar corrida PENDING_DRIVER', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue(RIDE);
      (prisma.ride.update as jest.Mock).mockResolvedValue({ ...RIDE, status: RideStatus.CANCELED });

      const result = await service.updateStatus(
        'ride-1',
        RideStatus.CANCELED,
        'passenger-1',
        'PASSENGER',
      );

      expect(result.status).toBe(RideStatus.CANCELED);
    });

    it('deve bloquear passageiro de alterar corrida de outro usuário', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue({ ...RIDE, passengerId: 'outro-user' });

      await expect(
        service.updateStatus('ride-1', RideStatus.CANCELED, 'passenger-1', 'PASSENGER'),
      ).rejects.toThrow(ForbiddenException);
    });

    it('deve bloquear passageiro de alterar status para não-CANCELED', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue(RIDE);

      await expect(
        service.updateStatus('ride-1', RideStatus.ACCEPTED, 'passenger-1', 'PASSENGER'),
      ).rejects.toThrow(ForbiddenException);
    });

    it('deve bloquear cancelamento de corrida IN_PROGRESS', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue({
        ...RIDE,
        status: RideStatus.IN_PROGRESS,
      });

      await expect(
        service.updateStatus('ride-1', RideStatus.CANCELED, 'passenger-1', 'PASSENGER'),
      ).rejects.toThrow(BadRequestException);
    });

    it('deve lançar NotFoundException para corrida inexistente', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue(null);

      await expect(
        service.updateStatus('inexistente', RideStatus.CANCELED, 'passenger-1', 'PASSENGER'),
      ).rejects.toThrow(NotFoundException);
    });

    it('admin deve poder alterar qualquer status', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue(RIDE);
      (prisma.ride.update as jest.Mock).mockResolvedValue({ ...RIDE, status: RideStatus.ACCEPTED });

      const result = await service.updateStatus('ride-1', RideStatus.ACCEPTED, 'admin-1', 'ADMIN');
      expect(result.status).toBe(RideStatus.ACCEPTED);
    });
  });

  // ──────────────────────────────────────────────
  // confirmPayment / markPaymentNotReceived — TA8
  // ──────────────────────────────────────────────
  describe('confirmPayment (TA8/TA9)', () => {
    const rideWithDriver = { ...RIDE, driverId: 'driver-1' };

    it('deve confirmar pagamento recebido', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue(rideWithDriver);
      (prisma.ride.update as jest.Mock).mockResolvedValue({
        ...rideWithDriver,
        paymentStatus: PaymentStatus.RECEIVED,
        paymentConfirmedAt: new Date(),
      });

      const result = await service.confirmPayment('ride-1', { userId: 'driver-1', role: 'DRIVER' });
      expect(result.paymentStatus).toBe(PaymentStatus.RECEIVED);
      expect(result.ok).toBe(true);
    });

    it('deve registrar pagamento não recebido (TA8)', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue(rideWithDriver);
      (prisma.ride.update as jest.Mock).mockResolvedValue({
        ...rideWithDriver,
        paymentStatus: PaymentStatus.NOT_RECEIVED,
        paymentReportedAt: new Date(),
      });

      const result = await service.markPaymentNotReceived('ride-1', {
        userId: 'driver-1',
        role: 'DRIVER',
      });
      expect(result.paymentStatus).toBe(PaymentStatus.NOT_RECEIVED);
      expect(result.ok).toBe(true);
    });

    it('deve bloquear usuário não-motorista de confirmar pagamento', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue(rideWithDriver);

      await expect(
        service.confirmPayment('ride-1', { userId: 'outro-user', role: 'PASSENGER' }),
      ).rejects.toThrow(ForbiddenException);
    });

    it('deve bloquear pagamento em corrida cancelada', async () => {
      (prisma.ride.findUnique as jest.Mock).mockResolvedValue({
        ...rideWithDriver,
        status: RideStatus.CANCELED,
      });

      await expect(
        service.confirmPayment('ride-1', { userId: 'driver-1', role: 'DRIVER' }),
      ).rejects.toThrow(BadRequestException);
    });
  });

  // ──────────────────────────────────────────────
  // validateCoupon
  // ──────────────────────────────────────────────
  describe('validateCoupon', () => {
    it('deve retornar cupom válido', async () => {
      (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue({
        id: 'promo-1',
        code: 'PROMO10',
        active: true,
        maxUses: null,
        usedCount: 0,
        expiresAt: null,
        discountPercent: 10,
        discountCents: null,
      });

      const result = await service.validateCoupon('PROMO10');
      expect(result.valid).toBe(true);
      expect(result.discountPercent).toBe(10);
    });

    it('deve lançar exceção para cupom não encontrado', async () => {
      (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue(null);
      await expect(service.validateCoupon('INEXISTENTE')).rejects.toThrow(BadRequestException);
    });

    it('deve lançar exceção para cupom inativo', async () => {
      (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue({
        id: 'promo-1',
        code: 'INATIVO',
        active: false,
        maxUses: null,
        usedCount: 0,
        expiresAt: null,
        discountPercent: 10,
        discountCents: null,
      });
      await expect(service.validateCoupon('INATIVO')).rejects.toThrow(BadRequestException);
    });

    it('deve lançar exceção para cupom esgotado', async () => {
      (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue({
        id: 'promo-1',
        code: 'ESGOTADO',
        active: true,
        maxUses: 5,
        usedCount: 5,
        expiresAt: null,
        discountPercent: 10,
        discountCents: null,
      });
      await expect(service.validateCoupon('ESGOTADO')).rejects.toThrow(BadRequestException);
    });

    it('deve lançar exceção para cupom expirado', async () => {
      (prisma.promoCode.findFirst as jest.Mock).mockResolvedValue({
        id: 'promo-1',
        code: 'EXPIRADO',
        active: true,
        maxUses: null,
        usedCount: 0,
        expiresAt: new Date('2020-01-01'),
        discountPercent: 10,
        discountCents: null,
      });
      await expect(service.validateCoupon('EXPIRADO')).rejects.toThrow(BadRequestException);
    });
  });
});
