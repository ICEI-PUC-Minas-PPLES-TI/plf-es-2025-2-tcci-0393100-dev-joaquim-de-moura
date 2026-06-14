import { Test, TestingModule } from '@nestjs/testing';
import { BadRequestException, NotFoundException } from '@nestjs/common';
import { DriverApprovalStatus, UserRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { NotificationService } from '../notification/notification.service';
import { AdminService } from './admin.service';

const USER_PASSENGER = {
  id: 'user-1',
  name: 'Mariana',
  phone: '31999990001',
  email: 'mariana@test.com',
  role: UserRole.PASSENGER,
  blocked: false,
  createdAt: new Date(),
};

const USER_DRIVER = { ...USER_PASSENGER, id: 'driver-user-1', role: UserRole.DRIVER };
const USER_ADMIN = { ...USER_PASSENGER, id: 'admin-1', role: UserRole.ADMIN };

const DRIVER_PROFILE = {
  id: 'dp-1',
  userId: 'driver-user-1',
  approvalStatus: DriverApprovalStatus.PENDING,
  online: false,
  available: false,
  rejectionReason: null,
  createdAt: new Date(),
};

describe('AdminService', () => {
  let service: AdminService;
  let prisma: jest.Mocked<PrismaService>;
  let notification: jest.Mocked<NotificationService>;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AdminService,
        {
          provide: PrismaService,
          useValue: {
            user: {
              findUnique: jest.fn(),
              findMany: jest.fn(),
              update: jest.fn(),
            },
            driverProfile: {
              findUnique: jest.fn(),
              findMany: jest.fn(),
              update: jest.fn(),
              updateMany: jest.fn(),
            },
            ride: {
              findMany: jest.fn(),
              groupBy: jest.fn(),
              count: jest.fn(),
              aggregate: jest.fn(),
            },
            review: { groupBy: jest.fn() },
            supportTicket: { groupBy: jest.fn(), count: jest.fn() },
            driverSettlement: {
              create: jest.fn(),
              delete: jest.fn(),
              findMany: jest.fn(),
              aggregate: jest.fn(),
            },
            driverPaymentRequest: {
              findFirst: jest.fn(),
              findMany: jest.fn(),
              update: jest.fn(),
            },
            pricingConfig: {
              findMany: jest.fn(),
              create: jest.fn(),
              update: jest.fn(),
              delete: jest.fn(),
            },
            operationRegion: {
              findMany: jest.fn(),
              create: jest.fn(),
              update: jest.fn(),
              delete: jest.fn(),
            },
            systemConfig: {
              findFirst: jest.fn(),
              findUnique: jest.fn(),
              upsert: jest.fn(),
            },
            promoCode: {
              findMany: jest.fn(),
              create: jest.fn(),
              update: jest.fn(),
              delete: jest.fn(),
            },
            $transaction: jest.fn(),
          },
        },
        {
          provide: NotificationService,
          useValue: { sendToUser: jest.fn() },
        },
      ],
    }).compile();

    service = module.get<AdminService>(AdminService);
    prisma = module.get(PrismaService);
    notification = module.get(NotificationService);
  });

  it('deve ser instanciado corretamente', () => {
    expect(service).toBeDefined();
  });

  // ──────────────────────────────────────────────
  // blockUser — TA11: bloquear usuário
  // ──────────────────────────────────────────────
  describe('blockUser (TA11)', () => {
    it('deve bloquear passageiro ativo', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValueOnce(USER_PASSENGER);
      (prisma.$transaction as jest.Mock).mockImplementation(async (fn) => {
        const tx = {
          user: { update: jest.fn().mockResolvedValueOnce({ ...USER_PASSENGER, blocked: true }) },
          driverProfile: { updateMany: jest.fn() },
        };
        return fn(tx);
      });

      const result = await service.blockUser('user-1');
      expect(result.blocked).toBe(true);
    });

    it('deve bloquear motorista e forçar offline', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValueOnce(USER_DRIVER);
      let driverProfileUpdateManyCalled = false;
      (prisma.$transaction as jest.Mock).mockImplementation(async (fn) => {
        const tx = {
          user: { update: jest.fn().mockResolvedValueOnce({ ...USER_DRIVER, blocked: true }) },
          driverProfile: {
            updateMany: jest.fn().mockImplementation(() => {
              driverProfileUpdateManyCalled = true;
              return Promise.resolve({ count: 1 });
            }),
          },
        };
        return fn(tx);
      });

      await service.blockUser('driver-user-1');
      expect(driverProfileUpdateManyCalled).toBe(true);
    });

    it('deve lançar BadRequestException ao tentar bloquear administrador', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValueOnce(USER_ADMIN);

      await expect(service.blockUser('admin-1')).rejects.toThrow(BadRequestException);
    });

    it('deve lançar NotFoundException para usuário inexistente', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValueOnce(null);

      await expect(service.blockUser('inexistente')).rejects.toThrow(NotFoundException);
    });
  });

  // ──────────────────────────────────────────────
  // unblockUser
  // ──────────────────────────────────────────────
  describe('unblockUser', () => {
    it('deve desbloquear usuário', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValueOnce({
        ...USER_PASSENGER,
        blocked: true,
      });
      (prisma.user.update as jest.Mock).mockResolvedValueOnce({
        ...USER_PASSENGER,
        blocked: false,
        blockedAt: null,
      });

      const result = await service.unblockUser('user-1');
      expect(result.blocked).toBe(false);
    });

    it('deve lançar NotFoundException para usuário inexistente', async () => {
      (prisma.user.findUnique as jest.Mock).mockResolvedValueOnce(null);

      await expect(service.unblockUser('inexistente')).rejects.toThrow(NotFoundException);
    });
  });

  // ──────────────────────────────────────────────
  // approveDriver — TA4: aprovar cadastro motorista
  // ──────────────────────────────────────────────
  describe('approveDriver', () => {
    it('deve aprovar cadastro de motorista e notificar', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(DRIVER_PROFILE);
      (prisma.driverProfile.update as jest.Mock).mockResolvedValueOnce({
        ...DRIVER_PROFILE,
        approvalStatus: DriverApprovalStatus.APPROVED,
        approvedAt: new Date(),
      });

      const result = await service.approveDriver('dp-1');

      expect(result.approvalStatus).toBe(DriverApprovalStatus.APPROVED);
      expect(notification.sendToUser).toHaveBeenCalledWith(
        DRIVER_PROFILE.userId,
        expect.stringContaining('aprovado'),
        expect.any(String),
        expect.objectContaining({ event: 'DRIVER_APPROVED' }),
      );
    });

    it('deve lançar NotFoundException para perfil inexistente', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(null);

      await expect(service.approveDriver('inexistente')).rejects.toThrow(NotFoundException);
    });
  });

  // ──────────────────────────────────────────────
  // rejectDriver
  // ──────────────────────────────────────────────
  describe('rejectDriver', () => {
    it('deve rejeitar cadastro com motivo e notificar motorista', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(DRIVER_PROFILE);
      (prisma.driverProfile.update as jest.Mock).mockResolvedValueOnce({
        ...DRIVER_PROFILE,
        approvalStatus: DriverApprovalStatus.REJECTED,
        rejectionReason: 'CNH inválida',
        online: false,
        available: false,
      });

      const result = await service.rejectDriver('dp-1', 'CNH inválida');

      expect(result.approvalStatus).toBe(DriverApprovalStatus.REJECTED);
      expect(notification.sendToUser).toHaveBeenCalledWith(
        DRIVER_PROFILE.userId,
        expect.any(String),
        expect.stringContaining('CNH inválida'),
        expect.objectContaining({ event: 'DRIVER_REJECTED' }),
      );
    });

    it('deve lançar BadRequestException sem motivo de rejeição', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(DRIVER_PROFILE);

      await expect(service.rejectDriver('dp-1', '')).rejects.toThrow(BadRequestException);
    });

    it('deve lançar NotFoundException para perfil inexistente', async () => {
      (prisma.driverProfile.findUnique as jest.Mock).mockResolvedValueOnce(null);

      await expect(service.rejectDriver('inexistente', 'motivo')).rejects.toThrow(NotFoundException);
    });
  });
});
