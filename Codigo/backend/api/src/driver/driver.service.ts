import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateDriverStatusDto } from './dto/update-driver-status.dto';
import { RideStatus } from '@prisma/client';

@Injectable()
export class DriverService {
  constructor(private prisma: PrismaService) {}

  async getDriverMe(userId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
      include: {
        user: true,
      },
    });

    if (!driver) {
      throw new NotFoundException('Driver profile não encontrado');
    }

    return {
      id: driver.id,
      name: driver.user.name,
      phone: driver.user.phone,
      online: driver.online,
      approvalStatus: driver.approvalStatus,
      rejectionReason: driver.rejectionReason,
    };
  }

  // motorista ficar online/offline
  async updateStatus(userId: string, dto: UpdateDriverStatusDto) {
    console.log('SERVICE updateStatus');
    console.log('userId:', userId);
    console.log('dto:', dto);

    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
    });

    if (!driver) {
      throw new NotFoundException('Driver profile não encontrado');
    }

    if (driver.approvalStatus !== 'APPROVED') {
      throw new BadRequestException('Motorista ainda não aprovado pelo administrador');
    }

    const updated = await this.prisma.driverProfile.update({
      where: { id: driver.id },
      data: {
        online: dto.online,
        available: dto.online,
      },
    });

    return {
      driverId: updated.id,
      online: updated.online,
      message: updated.online ? 'Motorista online' : 'Motorista offline',
    };
  }

  // buscar corridas pendentes
  async getPendingRides(userId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
    });

    if (!driver) {
      throw new NotFoundException('Driver profile não encontrado');
    }

    if (!driver.online) {
      return [];
    }

    const rides = await this.prisma.ride.findMany({
      where: {
        status: RideStatus.PENDING_DRIVER,
        rejections: {
          none: {
            driverId: driver.id,
          },
        },
      },
      include: {
        passenger: true,
      },
      orderBy: {
        createdAt: 'asc',
      },
    });

    return rides.map((ride) => ({
      rideId: ride.id,
      passengerName: ride.passenger.name,
      originAddress: ride.originAddress,
      destinationAddress: ride.destinationAddress,
      price: ride.estimatedFareCents,
      distanceMeters: ride.distanceMeters,
      status: ride.status,
    }));
  }

  // aceitar corrida
  async acceptRide(userId: string, rideId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
    });

    if (!driver) {
      throw new NotFoundException('Driver profile não encontrado');
    }

    if (!driver.online) {
      throw new BadRequestException('Motorista está offline');
    }

    const ride = await this.prisma.ride.findUnique({
      where: { id: rideId },
    });

    if (!ride) {
      throw new NotFoundException('Corrida não encontrada');
    }

    if (ride.status !== RideStatus.PENDING_DRIVER) {
      throw new BadRequestException('Corrida não está disponível');
    }

    const updated = await this.prisma.ride.updateMany({
      where: {
        id: rideId,
        status: RideStatus.PENDING_DRIVER,
        driverId: null,
      },
      data: {
        driverId: driver.userId,
        status: RideStatus.ACCEPTED,
      },
    });

    if (updated.count === 0) {
      throw new BadRequestException(
        'Corrida já foi aceita por outro motorista',
      );
    }

    return {
      success: true,
      message: 'Corrida aceita com sucesso',
    };
  }

  // recusar corrida
  async rejectRide(userId: string, rideId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
    });

    if (!driver) {
      throw new NotFoundException('Driver profile não encontrado');
    }

    const ride = await this.prisma.ride.findUnique({
      where: { id: rideId },
    });

    if (!ride) {
      throw new NotFoundException('Corrida não encontrada');
    }

    if (ride.status !== RideStatus.PENDING_DRIVER) {
      throw new BadRequestException('Corrida não pode ser recusada');
    }

    await this.prisma.rideRejection.upsert({
      where: {
        rideId_driverId: {
          rideId,
          driverId: driver.id,
        },
      },
      update: {},
      create: {
        rideId,
        driverId: driver.id,
      },
    });

    return {
      success: true,
      message: 'Corrida recusada',
    };
  }
}