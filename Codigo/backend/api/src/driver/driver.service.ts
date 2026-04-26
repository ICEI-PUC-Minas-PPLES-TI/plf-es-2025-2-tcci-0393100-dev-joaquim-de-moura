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
      userId: driver.userId,
      name: driver.user.name,
      phone: driver.user.phone,
      online: driver.online,
      available: driver.available,
      approvalStatus: driver.approvalStatus,
      rejectionReason: driver.rejectionReason,
      pixQrPayload: driver.pixQrPayload,
    };
  }

  async updateStatus(userId: string, dto: UpdateDriverStatusDto) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
    });

    if (!driver) {
      throw new NotFoundException('Driver profile não encontrado');
    }

    if (driver.approvalStatus !== 'APPROVED') {
      throw new BadRequestException(
        'Motorista ainda não aprovado pelo administrador',
      );
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
      available: updated.available,
      message: updated.online ? 'Motorista online' : 'Motorista offline',
    };
  }

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
      passengerId: ride.passengerId,
      passengerName: ride.passenger.name,
      passengerPhone: ride.passenger.phone,
      originLat: ride.originLat,
      originLng: ride.originLng,
      destLat: ride.destLat,
      destLng: ride.destLng,
      originAddress: ride.originAddress,
      destinationAddress: ride.destinationAddress,
      price: ride.estimatedFareCents,
      distanceMeters: ride.distanceMeters,
      durationSeconds: ride.durationSeconds,
      status: ride.status,
      createdAt: ride.createdAt,
    }));
  }

  async acceptRide(userId: string, rideId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
      include: {
        user: true,
      },
    });

    if (!driver) {
      throw new NotFoundException('Driver profile não encontrado');
    }

    if (!driver.online) {
      throw new BadRequestException('Motorista está offline');
    }

    if (driver.approvalStatus !== 'APPROVED') {
      throw new BadRequestException('Motorista ainda não aprovado');
    }

    const ride = await this.prisma.ride.findUnique({
      where: { id: rideId },
      include: {
        passenger: true,
      },
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

    const acceptedRide = await this.prisma.ride.findUnique({
      where: { id: rideId },
      include: {
        passenger: true,
      },
    });

    return {
      success: true,
      message: 'Corrida aceita com sucesso',
      rideId: acceptedRide?.id,
      passengerId: acceptedRide?.passengerId,
      passengerName: acceptedRide?.passenger?.name,
      passengerPhone: acceptedRide?.passenger?.phone,
      originLat: acceptedRide?.originLat,
      originLng: acceptedRide?.originLng,
      destLat: acceptedRide?.destLat,
      destLng: acceptedRide?.destLng,
      originAddress: acceptedRide?.originAddress,
      destinationAddress: acceptedRide?.destinationAddress,
      status: acceptedRide?.status,
    };
  }

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

  async getCurrentRide(userId: string) {
  console.log('=== SERVICE getCurrentRide START ===');
  console.log('SERVICE getCurrentRide userId =>', userId);

  if (!userId) {
    console.log('SERVICE getCurrentRide => userId inválido');
    throw new BadRequestException('Usuário autenticado inválido');
  }

  const driver = await this.prisma.driverProfile.findUnique({
    where: { userId },
  });

  console.log('SERVICE getCurrentRide driver =>', driver);

  if (!driver) {
    console.log('SERVICE getCurrentRide => driver não encontrado');
    throw new NotFoundException('Driver profile não encontrado');
  }

  const ride = await this.prisma.ride.findFirst({
    where: {
      driverId: userId,
      status: {
        in: [RideStatus.ACCEPTED, RideStatus.IN_PROGRESS],
      },
    },
    include: {
      passenger: true,
    },
    orderBy: {
      createdAt: 'desc',
    },
  });

  console.log('SERVICE getCurrentRide ride =>', ride);
  console.log('=== SERVICE getCurrentRide END ===');

  if (!ride) {
    return null;
  }

  return {
    rideId: ride.id,
    passengerId: ride.passengerId,
    passengerName: ride.passenger?.name,
    passengerPhone: ride.passenger?.phone,
    originLat: ride.originLat,
    originLng: ride.originLng,
    destLat: ride.destLat,
    destLng: ride.destLng,
    originAddress: ride.originAddress,
    destinationAddress: ride.destinationAddress,
    price: ride.estimatedFareCents,
    distanceMeters: ride.distanceMeters,
    durationSeconds: ride.durationSeconds,
    status: ride.status,
  };
}

  async startRide(userId: string, rideId: string) {
    const ride = await this.prisma.ride.findFirst({
      where: {
        id: rideId,
        driverId: userId,
      },
      include: {
        passenger: true,
      },
    });

    if (!ride) {
      throw new NotFoundException('Corrida não encontrada');
    }

    if (ride.status !== RideStatus.ACCEPTED) {
      throw new BadRequestException('A corrida ainda não pode ser iniciada');
    }

    const updatedRide = await this.prisma.ride.update({
      where: { id: rideId },
      data: {
        status: RideStatus.IN_PROGRESS,
      },
      include: {
        passenger: true,
      },
    });

    return {
      success: true,
      message: 'Corrida iniciada com sucesso',
      rideId: updatedRide.id,
      passengerId: updatedRide.passengerId,
      passengerName: updatedRide.passenger?.name,
      passengerPhone: updatedRide.passenger?.phone,
      originLat: updatedRide.originLat,
      originLng: updatedRide.originLng,
      destLat: updatedRide.destLat,
      destLng: updatedRide.destLng,
      originAddress: updatedRide.originAddress,
      destinationAddress: updatedRide.destinationAddress,
      price: updatedRide.estimatedFareCents,
      distanceMeters: updatedRide.distanceMeters,
      durationSeconds: updatedRide.durationSeconds,
      status: updatedRide.status,
    };
  }

  async finishRide(userId: string, rideId: string) {
    const ride = await this.prisma.ride.findFirst({
      where: {
        id: rideId,
        driverId: userId,
      },
      include: {
        passenger: true,
        driver: {
          include: {
            driverProfile: true,
          },
        },
      },
    });

    if (!ride) {
      throw new NotFoundException('Corrida não encontrada');
    }

    if (ride.status !== RideStatus.IN_PROGRESS) {
      throw new BadRequestException('A corrida ainda não pode ser finalizada');
    }

    const updatedRide = await this.prisma.ride.update({
      where: { id: rideId },
      data: {
        status: RideStatus.FINISHED,
      },
      include: {
        passenger: true,
        driver: {
          include: {
            driverProfile: true,
          },
        },
      },
    });

    return {
      success: true,
      message: 'Corrida finalizada com sucesso',
      rideId: updatedRide.id,
      passengerId: updatedRide.passengerId,
      passengerName: updatedRide.passenger?.name,
      passengerPhone: updatedRide.passenger?.phone,
      originLat: updatedRide.originLat,
      originLng: updatedRide.originLng,
      destLat: updatedRide.destLat,
      destLng: updatedRide.destLng,
      originAddress: updatedRide.originAddress,
      destinationAddress: updatedRide.destinationAddress,
      price: updatedRide.estimatedFareCents,
      distanceMeters: updatedRide.distanceMeters,
      durationSeconds: updatedRide.durationSeconds,
      status: updatedRide.status,
      driverPixQrPayload:
        updatedRide.driver?.driverProfile?.pixQrPayload ?? null,
    };
  }
}