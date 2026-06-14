import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException, NotFoundException } from '@nestjs/common';
import { DriverApprovalStatus, RideStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { RealtimeService } from '../realtime/realtime.service';
import { NotificationService } from '../notification/notification.service';
import { DriverService } from './driver.service';

const APPROVED_DRIVER = {
  id: 'dp-1',
  userId: 'driver-1',
  online: false,
  available: false,
  approvalStatus: DriverApprovalStatus.APPROVED,
  currentLat: -19.7,
  currentLng: -44.8,
  cnhNumber: '12345',
  cnhCategory: 'B',
  cnhExpiresAt: null,
  cnhImageUrl: null,
  hasEar: false,
  cpf: null,
  pixKey: 'motorista@pix.com',
  pixQrCodeUrl: null,
  pixQrPayload: null,
  vehicleModel: 'Onix',
  vehiclePlate: 'ABC-1234',
  vehicleColor: 'Branco',
  vehicleYear: 2020,
  vehicleCapacity: 4,
  profilePhotoUrl: null,
  rejectionReason: null,
  createdAt: new Date(),
  updatedAt: new Date(),
  user: { name: 'Carlos Motorista', phone: '31988880001' },
};

const PENDING_RIDE = {
  id: 'ride-1',
  passengerId: 'pass-1',
  driverId: null,
  status: RideStatus.PENDING_DRIVER,
  originLat: -19.7,
  originLng: -44.8,
  destLat: -19.75,
  destLng: -44.85,
  originAddress: 'Rua A',
  destinationAddress: 'Rua B',
  estimatedFareCents: 2000,
  distanceMeters: 5000,
  durationSeconds: 600,
  paymentMethod: 'CASH',
  createdAt: new Date(),
  passenger: {
    name: 'Mariana',
    phone: '31999990001',
    profilePhotoUrl: null,
    _count: { passengerRides: 3 },
  },
};

describe('DriverService', () => {
  let service: DriverService;
  let prisma: jest.Mocked<PrismaService>;
  let realtime: jest.Mocked<RealtimeService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        DriverService,
        {
          provide: PrismaService,
          useValue: {
            driverProfile: {
              findUnique: jest.fn(),
              update: jest.fn(),
              updateMany: jest.fn(),
            },
            ride: {
              findUnique: jest.fn(),
              findMany: jest.fn(),
              updateMany: jest.fn(),
              update: jest.fn(),
              aggregate: jest.fn(),
            },
            systemConfig: { findUnique: jest.fn() },
            rideRejection: { create: jest.fn() },
            review: { aggregate: jest.fn() },
            driverSettlement: { findMany: jest.fn(), aggregate: jest.fn(), create: jest.fn() },
            driverPaymentRequest: {
              findFirst: jest.fn(),
              create: jest.fn(),
              findMany: jest.fn(),
            },
            rideAuditLog: { create: jest.fn() },
          },
        },
        {
          provide: RealtimeService,
          useValue: {
            emitRide: jest.fn(),
            emitToUser: jest.fn(),
            emitAdmin: jest.fn(),
          },
        },
        {
          provide: NotificationService,
          useValue: { sendToUser: jest.fn(), sendToUsers: jest.fn() },
        },
      ],
    }).compile();

    service = module.get<DriverService>(DriverService);
    prisma = module.get(PrismaService);
    realtime = module.get(RealtimeService);
  });

  it('deve ser instanciado corretamente', () => {
    expect(service).toBeDefined();
  });

  // ──────────────────────────────────────────────
  // updateStatus — TA6: online/offline (TI5)
  // ──────────────────────────────────────────────
  describe('updateStatus (TA6 / TI5)', () => {
    it('deve colocar motorista APROVADO online', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(APPROVED_DRIVER);
      (prisma.systemConfig.findUnique as jest.Mock).mockResolvedValueOnce(null);
      // getDebtBalance uses aggregate, no debt → allow online
      (prisma.ride.aggregate as jest.Mock).mockResolvedValueOnce({ _sum: { platformFeeCents: 0 } });
      (prisma.driverSettlement.aggregate as jest.Mock).mockResolvedValueOnce({ _sum: { amountCents: 0 } });
      (prisma.driverProfile.update as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        online: true,
        available: true,
      });

      const result = await service.updateStatus('driver-1', { online: true });

      expect(result.online).toBe(true);
      expect(result.available).toBe(true);
      expect(result.message).toContain('online');
    });

    it('deve colocar motorista offline e definir available=false', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        online: true,
        available: true,
      });
      (prisma.driverProfile.update as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        online: false,
        available: false,
      });

      const result = await service.updateStatus('driver-1', { online: false });

      expect(result.online).toBe(false);
      expect(result.available).toBe(false);
      expect(result.message).toContain('offline');
    });

    it('deve lançar NotFoundException para motorista não cadastrado', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(null);

      await expect(service.updateStatus('inexistente', { online: true })).rejects.toThrow(
        NotFoundException,
      );
    });

    it('deve lançar BadRequestException para motorista não aprovado', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        approvalStatus: DriverApprovalStatus.PENDING,
      });

      await expect(service.updateStatus('driver-1', { online: true })).rejects.toThrow(
        BadRequestException,
      );
    });

    it('deve bloquear ficar online quando dívida excede o limite', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(APPROVED_DRIVER);
      (prisma.systemConfig.findUnique as jest.Mock).mockResolvedValueOnce({
        key: 'DRIVER_DEBT_LIMIT_CENTS',
        value: '5000',
      });
      // getDebtBalance usa aggregate: taxas = 6000 cents, sem liquidações → dívida 6000 > limite 5000
      (prisma.ride.aggregate as jest.Mock).mockResolvedValueOnce({
        _sum: { platformFeeCents: 6000 },
      });
      (prisma.driverSettlement.aggregate as jest.Mock).mockResolvedValueOnce({
        _sum: { amountCents: 0 },
      });

      await expect(service.updateStatus('driver-1', { online: true })).rejects.toThrow(
        BadRequestException,
      );
    });
  });

  // ──────────────────────────────────────────────
  // acceptRide — TA4: motorista aceitar corrida
  // ──────────────────────────────────────────────
  describe('acceptRide (TA4)', () => {
    beforeEach(() => {
      (prisma.driverProfile.findUnique as jest.Mock)
        .mockResolvedValueOnce({ ...APPROVED_DRIVER, online: true, available: true })
        .mockResolvedValueOnce(APPROVED_DRIVER);
      (prisma.ride.findUnique as jest.Mock)
        .mockResolvedValueOnce({ ...PENDING_RIDE })
        .mockResolvedValueOnce({ ...PENDING_RIDE, driverId: 'driver-1', status: RideStatus.ACCEPTED });
      (prisma.ride.updateMany as jest.Mock).mockResolvedValueOnce({ count: 1 });
      (prisma.driverProfile.update as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        available: false,
      });
    });

    it('deve aceitar corrida e retornar dados da corrida aceita', async () => {
      const result = await service.acceptRide('driver-1', 'ride-1');
      expect(result).toBeDefined();
      expect(prisma.ride.updateMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: expect.objectContaining({ id: 'ride-1', status: RideStatus.PENDING_DRIVER }),
          data: expect.objectContaining({ status: RideStatus.ACCEPTED }),
        }),
      );
    });

    it('deve emitir evento realtime para o passageiro após aceite', async () => {
      await service.acceptRide('driver-1', 'ride-1');
      expect(realtime.emitRide).toHaveBeenCalledWith(
        'ride-1',
        'ride_status_update',
        expect.objectContaining({ status: 'ACCEPTED' }),
      );
    });

    it('deve tornar motorista indisponível após aceitar corrida (evita corrida dupla)', async () => {
      await service.acceptRide('driver-1', 'ride-1');
      expect(prisma.driverProfile.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: expect.objectContaining({ available: false }),
        }),
      );
    });

    it('deve lançar BadRequestException quando corrida já foi aceita por outro motorista', async () => {
      // Simulate race condition: updateMany returns count=0
      (prisma.driverProfile.findUnique as jest.Mock).mockReset();
      (prisma.driverProfile.findUnique as jest.Mock)
        .mockResolvedValueOnce({ ...APPROVED_DRIVER, online: true, available: true });
      (prisma.ride.findUnique as jest.Mock).mockReset();
      (prisma.ride.findUnique as jest.Mock).mockResolvedValueOnce({ ...PENDING_RIDE });
      (prisma.ride.updateMany as jest.Mock).mockReset();
      (prisma.ride.updateMany as jest.Mock).mockResolvedValueOnce({ count: 0 });

      await expect(service.acceptRide('driver-1', 'ride-1')).rejects.toThrow(BadRequestException);
    });

    it('deve lançar BadRequestException para motorista offline', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockReset();
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        online: false,
      });
      (prisma.ride.findUnique as jest.Mock).mockReset();
      (prisma.ride.updateMany as jest.Mock).mockReset();

      await expect(service.acceptRide('driver-1', 'ride-1')).rejects.toThrow(BadRequestException);
    });
  });

  // ──────────────────────────────────────────────
  // getPendingRides
  // ──────────────────────────────────────────────
  describe('getPendingRides', () => {
    it('deve retornar array vazio para motorista offline', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        online: false,
        available: false,
      });

      const result = await service.getPendingRides('driver-1');
      expect(result).toEqual([]);
    });

    it('deve retornar corridas ordenadas por proximidade', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce({
        ...APPROVED_DRIVER,
        online: true,
        available: true,
        currentLat: -19.7,
        currentLng: -44.8,
      });
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce([
        { ...PENDING_RIDE, id: 'ride-far', originLat: -20.0, originLng: -45.0 },
        { ...PENDING_RIDE, id: 'ride-near', originLat: -19.7, originLng: -44.8 },
      ]);

      const result = await service.getPendingRides('driver-1');

      expect(result[0].rideId).toBe('ride-near');
      expect(result[1].rideId).toBe('ride-far');
    });

    it('deve lançar NotFoundException para motorista não encontrado', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(null);

      await expect(service.getPendingRides('inexistente')).rejects.toThrow(NotFoundException);
    });
  });

  // ──────────────────────────────────────────────
  // updateLocation
  // ──────────────────────────────────────────────
  describe('updateLocation', () => {
    it('deve atualizar localização do motorista', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(APPROVED_DRIVER);
      (prisma.driverProfile.update as jest.Mock).mockResolvedValueOnce(APPROVED_DRIVER);

      const result = await service.updateLocation('driver-1', -19.71, -44.81);

      expect(result.ok).toBe(true);
      expect(prisma.driverProfile.update).toHaveBeenCalledWith(
        expect.objectContaining({
          data: { currentLat: -19.71, currentLng: -44.81 },
        }),
      );
    });

    it('deve lançar NotFoundException para motorista não encontrado', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(null);

      await expect(service.updateLocation('inexistente', -19.7, -44.8)).rejects.toThrow(
        NotFoundException,
      );
    });
  });
});
