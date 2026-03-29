import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { DriverApprovalStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class AdminService {
  constructor(private prisma: PrismaService) {}

  async listPendingDrivers() {
    return this.prisma.driverProfile.findMany({
      where: {
        approvalStatus: DriverApprovalStatus.PENDING,
      },
      include: {
        user: true,
      },
      orderBy: {
        createdAt: 'asc',
      },
    });
  }

  async approveDriver(driverProfileId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { id: driverProfileId },
    });

    if (!driver) {
      throw new NotFoundException('Motorista não encontrado');
    }

    return this.prisma.driverProfile.update({
      where: { id: driverProfileId },
      data: {
        approvalStatus: DriverApprovalStatus.APPROVED,
        approvedAt: new Date(),
        rejectionReason: null,
      },
    });
  }

  async rejectDriver(driverProfileId: string, reason: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { id: driverProfileId },
    });

    if (!driver) {
      throw new NotFoundException('Motorista não encontrado');
    }

    if (!reason || !reason.trim()) {
      throw new BadRequestException('Motivo da rejeição é obrigatório');
    }

    return this.prisma.driverProfile.update({
      where: { id: driverProfileId },
      data: {
        approvalStatus: DriverApprovalStatus.REJECTED,
        approvedAt: null,
        rejectionReason: reason,
        online: false,
        available: false,
      },
    });
  }
}