import { Test, TestingModule } from '@nestjs/testing';
import { RideStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { RealtimeService } from '../realtime/realtime.service';
import { RideExpiryService } from './ride-expiry.service';

describe('RideExpiryService — Cancelamento Automático', () => {
  let service: RideExpiryService;
  let prisma: jest.Mocked<PrismaService>;
  let realtime: jest.Mocked<RealtimeService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RideExpiryService,
        {
          provide: PrismaService,
          useValue: {
            ride: {
              findMany: jest.fn(),
              updateMany: jest.fn(),
            },
          },
        },
        {
          provide: RealtimeService,
          useValue: { emitToUser: jest.fn() },
        },
      ],
    }).compile();

    service = module.get<RideExpiryService>(RideExpiryService);
    prisma = module.get(PrismaService);
    realtime = module.get(RealtimeService);
  });

  // ──────────────────────────────────────────────
  // cancelStalePendingRides — PENDING_DRIVER > 10 min
  // ──────────────────────────────────────────────
  describe('cancelStalePendingRides', () => {
    it('não deve cancelar nada se não há corridas expiradas', async () => {
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce([]);

      await service.cancelStalePendingRides();

      expect(prisma.ride.updateMany).not.toHaveBeenCalled();
      expect(realtime.emitToUser).not.toHaveBeenCalled();
    });

    it('deve cancelar corridas PENDING_DRIVER com mais de 10 minutos', async () => {
      const staleRides = [
        { id: 'ride-1', passengerId: 'pass-1' },
        { id: 'ride-2', passengerId: 'pass-2' },
      ];
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce(staleRides);
      (prisma.ride.updateMany as jest.Mock).mockResolvedValueOnce({ count: 2 });

      await service.cancelStalePendingRides();

      expect(prisma.ride.updateMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { id: { in: ['ride-1', 'ride-2'] } },
          data: { status: RideStatus.CANCELED },
        }),
      );
    });

    it('deve notificar cada passageiro via realtime ao cancelar', async () => {
      const staleRides = [
        { id: 'ride-1', passengerId: 'pass-1' },
        { id: 'ride-2', passengerId: 'pass-2' },
      ];
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce(staleRides);
      (prisma.ride.updateMany as jest.Mock).mockResolvedValueOnce({ count: 2 });

      await service.cancelStalePendingRides();

      expect(realtime.emitToUser).toHaveBeenCalledTimes(2);
      expect(realtime.emitToUser).toHaveBeenCalledWith(
        'pass-1',
        'ride_status_update',
        expect.objectContaining({ rideId: 'ride-1', status: 'CANCELED' }),
      );
      expect(realtime.emitToUser).toHaveBeenCalledWith(
        'pass-2',
        'ride_status_update',
        expect.objectContaining({ rideId: 'ride-2', status: 'CANCELED' }),
      );
    });

    it('deve buscar somente corridas PENDING_DRIVER criadas antes do cutoff de 10 minutos', async () => {
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce([]);

      const before = Date.now();
      await service.cancelStalePendingRides();
      const after = Date.now();

      const findManyCall = (prisma.ride.findMany as jest.Mock).mock.calls[0][0];
      expect(findManyCall.where.status).toBe(RideStatus.PENDING_DRIVER);
      const cutoff: Date = findManyCall.where.createdAt.lt;
      const cutoffMs = cutoff.getTime();
      expect(cutoffMs).toBeGreaterThanOrEqual(before - 10 * 60 * 1000 - 100);
      expect(cutoffMs).toBeLessThanOrEqual(after - 10 * 60 * 1000 + 100);
    });
  });

  // ──────────────────────────────────────────────
  // cancelStaleAcceptedRides — ACCEPTED > 30 min
  // ──────────────────────────────────────────────
  describe('cancelStaleAcceptedRides', () => {
    it('não deve cancelar nada se não há corridas ACCEPTED expiradas', async () => {
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce([]);

      await service.cancelStaleAcceptedRides();

      expect(prisma.ride.updateMany).not.toHaveBeenCalled();
    });

    it('deve cancelar corridas ACCEPTED com mais de 30 minutos paradas', async () => {
      const staleRides = [{ id: 'ride-3', passengerId: 'pass-3' }];
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce(staleRides);
      (prisma.ride.updateMany as jest.Mock).mockResolvedValueOnce({ count: 1 });

      await service.cancelStaleAcceptedRides();

      expect(prisma.ride.updateMany).toHaveBeenCalledWith(
        expect.objectContaining({
          where: { id: { in: ['ride-3'] } },
          data: { status: RideStatus.CANCELED },
        }),
      );
    });

    it('deve notificar o passageiro com mensagem específica de motorista não chegou', async () => {
      const staleRides = [{ id: 'ride-3', passengerId: 'pass-3' }];
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce(staleRides);
      (prisma.ride.updateMany as jest.Mock).mockResolvedValueOnce({ count: 1 });

      await service.cancelStaleAcceptedRides();

      expect(realtime.emitToUser).toHaveBeenCalledWith(
        'pass-3',
        'ride_status_update',
        expect.objectContaining({
          status: 'CANCELED',
          reason: expect.stringContaining('Motorista'),
        }),
      );
    });

    it('deve buscar somente corridas ACCEPTED com updatedAt antes do cutoff de 30 minutos', async () => {
      (prisma.ride.findMany as jest.Mock).mockResolvedValueOnce([]);

      const before = Date.now();
      await service.cancelStaleAcceptedRides();
      const after = Date.now();

      const findManyCall = (prisma.ride.findMany as jest.Mock).mock.calls[0][0];
      expect(findManyCall.where.status).toBe(RideStatus.ACCEPTED);
      const cutoff: Date = findManyCall.where.updatedAt.lt;
      const cutoffMs = cutoff.getTime();
      expect(cutoffMs).toBeGreaterThanOrEqual(before - 30 * 60 * 1000 - 100);
      expect(cutoffMs).toBeLessThanOrEqual(after - 30 * 60 * 1000 + 100);
    });
  });
});
