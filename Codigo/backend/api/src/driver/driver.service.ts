import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateDriverStatusDto } from './dto/update-driver-status.dto';
import { UpdateDriverProfileDto } from './dto/update-driver-profile.dto';
import { PaymentMethod, PaymentRequestStatus, PaymentStatus, RideStatus } from '@prisma/client';
import { RealtimeService } from '../realtime/realtime.service';
import { buildPixPayload, makePixTxId } from '../payment/pix.util';
import { NotificationService } from '../notification/notification.service';

@Injectable()
export class DriverService {
  constructor(
    private prisma: PrismaService,
    private realtime: RealtimeService,
    private notification: NotificationService,
  ) {}

  private haversineMeters(
    lat1: number,
    lng1: number,
    lat2: number,
    lng2: number,
  ): number {
    const R = 6_371_000;
    const toRad = (value: number) => (value * Math.PI) / 180;
    const dLat = toRad(lat2 - lat1);
    const dLng = toRad(lng2 - lng1);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  async getDriverMe(userId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
      include: { user: true },
    });

    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    return {
      id: driver.id,
      userId: driver.userId,
      name: driver.user.name,
      phone: driver.user.phone,
      online: driver.online,
      available: driver.available,
      approvalStatus: driver.approvalStatus,
      rejectionReason: driver.rejectionReason,
      profilePhotoUrl: driver.profilePhotoUrl ?? null,
      cnhNumber: driver.cnhNumber,
      cnhCategory: driver.cnhCategory,
      cnhExpiresAt: driver.cnhExpiresAt?.toISOString().substring(0, 10) ?? null,
      cnhImageUrl: driver.cnhImageUrl ?? null,
      hasEar: driver.hasEar,
      cpf: driver.cpf ?? null,
      pixKey: driver.pixKey,
      pixQrCodeUrl: driver.pixQrCodeUrl,
      pixQrPayload: driver.pixQrPayload,
      vehicleModel: driver.vehicleModel,
      vehiclePlate: driver.vehiclePlate,
      vehicleColor: driver.vehicleColor,
      vehicleYear: driver.vehicleYear ?? null,
      vehicleCapacity: driver.vehicleCapacity ?? null,
    };
  }

  async updateCnhImageUrl(userId: string, cnhImageUrl: string) {
    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');
    await this.prisma.driverProfile.update({ where: { userId }, data: { cnhImageUrl } });
    return { cnhImageUrl };
  }

  async updateDriverPhotoUrl(userId: string, photoUrl: string) {
    const updated = await this.prisma.driverProfile.update({
      where: { userId },
      data: { profilePhotoUrl: photoUrl },
      include: { user: true },
    });
    return {
      id: updated.id,
      userId: updated.userId,
      name: updated.user.name,
      phone: updated.user.phone,
      online: updated.online,
      available: updated.available,
      approvalStatus: updated.approvalStatus,
      rejectionReason: updated.rejectionReason,
      profilePhotoUrl: updated.profilePhotoUrl ?? null,
      cnhNumber: updated.cnhNumber,
      cnhCategory: updated.cnhCategory,
      hasEar: updated.hasEar,
      pixKey: updated.pixKey,
      pixQrCodeUrl: updated.pixQrCodeUrl,
      pixQrPayload: updated.pixQrPayload,
      vehicleModel: updated.vehicleModel,
      vehiclePlate: updated.vehiclePlate,
      vehicleColor: updated.vehicleColor,
    };
  }

  async updateStatus(userId: string, dto: UpdateDriverStatusDto) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
    });

    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    if (driver.approvalStatus !== 'APPROVED') {
      throw new BadRequestException('Motorista ainda não aprovado pelo administrador');
    }

    if (dto.online) {
      const balance = await this.getDebtBalance(userId);
      const limitConfig = await this.prisma.systemConfig.findUnique({ where: { key: 'DRIVER_DEBT_LIMIT_CENTS' } });
      const limitCents = parseInt(limitConfig?.value ?? '5000', 10);
      if (balance > limitCents) {
        throw new BadRequestException(
          `Saldo pendente com a plataforma (R$ ${(balance / 100).toFixed(2)}) excede o limite permitido. Regularize o débito para ficar online.`,
        );
      }
    }

    // Determine the new available value:
    //  - going offline → always false
    //  - going online and dto.available is explicitly set → use dto.available
    //  - going online without dto.available → default to true (original behaviour)
    let newAvailable: boolean;
    if (!dto.online) {
      newAvailable = false;
    } else if (dto.available !== undefined) {
      newAvailable = dto.available;
    } else {
      newAvailable = true;
    }

    const updated = await this.prisma.driverProfile.update({
      where: { id: driver.id },
      data: {
        online: dto.online,
        available: newAvailable,
      },
    });

    return {
      driverId: updated.id,
      online: updated.online,
      available: updated.available,
      message: updated.online ? 'Motorista online' : 'Motorista offline',
    };
  }

  // Motorista envia localização em tempo real
  async updateLocation(userId: string, lat: number, lng: number) {
    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    await this.prisma.driverProfile.update({
      where: { id: driver.id },
      data: { currentLat: lat, currentLng: lng },
    });

    return { ok: true };
  }

  async getPendingRides(userId: string) {
    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');
    if (!driver.online || !driver.available) return [];

    const rides = await this.prisma.ride.findMany({
      where: {
        status: RideStatus.PENDING_DRIVER,
        rejections: { none: { driverId: driver.id } },
      },
      include: {
        passenger: {
          include: {
            _count: { select: { passengerRides: true } }
          }
        }
      },
    });

    return rides
      .map((ride) => {
        const pickupDistanceMeters =
          driver.currentLat != null && driver.currentLng != null
            ? this.haversineMeters(driver.currentLat, driver.currentLng, ride.originLat, ride.originLng)
            : null;

        return {
          rideId: ride.id,
          passengerId: ride.passengerId,
          passengerName: ride.passenger.name,
          passengerPhone: ride.passenger.phone,
          passengerPhotoUrl: ride.passenger.profilePhotoUrl ?? null,
          originLat: ride.originLat,
          originLng: ride.originLng,
          destLat: ride.destLat,
          destLng: ride.destLng,
          originAddress: ride.originAddress,
          destinationAddress: ride.destinationAddress,
          price: ride.estimatedFareCents,
          distanceMeters: ride.distanceMeters,
          durationSeconds: ride.durationSeconds,
          paymentMethod: ride.paymentMethod,
          status: ride.status,
          createdAt: ride.createdAt,
          passengerTrips: ride.passenger._count.passengerRides,
          pickupDistanceMeters,
        };
      })
      .sort((a, b) => {
        const ad = a.pickupDistanceMeters ?? Number.MAX_SAFE_INTEGER;
        const bd = b.pickupDistanceMeters ?? Number.MAX_SAFE_INTEGER;
        if (ad !== bd) return ad - bd;
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      });
  }

  async acceptRide(userId: string, rideId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { userId },
      include: { user: true },
    });

    if (!driver) throw new NotFoundException('Driver profile não encontrado');
    if (!driver.online) throw new BadRequestException('Motorista está offline');
    if (!driver.available) throw new BadRequestException('Motorista está indisponível para novas corridas');
    if (driver.approvalStatus !== 'APPROVED') {
      throw new BadRequestException('Motorista ainda não aprovado');
    }

    const ride = await this.prisma.ride.findUnique({
      where: { id: rideId },
      include: { passenger: true },
    });

    if (!ride) throw new NotFoundException('Corrida não encontrada');
    if (ride.status !== RideStatus.PENDING_DRIVER) {
      throw new BadRequestException('Corrida não está disponível');
    }

    const updated = await this.prisma.ride.updateMany({
      where: { id: rideId, status: RideStatus.PENDING_DRIVER, driverId: null },
      data: { driverId: driver.userId, status: RideStatus.ACCEPTED, acceptedAt: new Date() },
    });

    if (updated.count === 0) {
      throw new BadRequestException('Corrida já foi aceita por outro motorista');
    }

    // Motorista fica indisponível para novas corridas enquanto está em atendimento
    await this.prisma.driverProfile.update({
      where: { id: driver.id },
      data: { available: false },
    });

    const acceptedRide = await this.prisma.ride.findUnique({
      where: { id: rideId },
      include: { passenger: true },
    });

    // Notify passenger in real-time that a driver accepted
    const driverProfile = await this.prisma.driverProfile.findUnique({
      where: { userId },
    });
    this.realtime.emitRide(rideId, 'ride_status_update', {
      rideId,
      status: 'ACCEPTED',
      driverName: driver.user.name,
      driverLat: driverProfile?.currentLat ?? null,
      driverLng: driverProfile?.currentLng ?? null,
    });

    this.notification.sendToUser(
      ride.passengerId,
      'Motorista a caminho!',
      `${driver.user.name ?? 'Seu motorista'} aceitou sua corrida e está indo até você.`,
      { rideId, event: 'ACCEPTED' },
    );

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
    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    const ride = await this.prisma.ride.findUnique({ where: { id: rideId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');
    if (ride.status !== RideStatus.PENDING_DRIVER) {
      throw new BadRequestException('Corrida não pode ser recusada');
    }

    await this.prisma.rideRejection.upsert({
      where: { rideId_driverId: { rideId, driverId: driver.id } },
      update: {},
      create: { rideId, driverId: driver.id },
    });

    return { success: true, message: 'Corrida recusada' };
  }

  async getCurrentRide(userId: string) {
    if (!userId) throw new BadRequestException('Usuário autenticado inválido');

    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    const ride = await this.prisma.ride.findFirst({
      where: {
        driverId: userId,
        status: { in: [RideStatus.ACCEPTED, RideStatus.DRIVER_ARRIVING, RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS] },
      },
      include: { passenger: true },
      orderBy: { createdAt: 'desc' },
    });

    if (!ride) return null;

    return {
      rideId: ride.id,
      passengerId: ride.passengerId,
      passengerName: ride.passenger?.name,
      passengerPhone: ride.passenger?.phone,
      passengerPhotoUrl: ride.passenger?.profilePhotoUrl ?? null,
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
      paymentMethod: ride.paymentMethod,
      paymentStatus: ride.paymentStatus,
      paymentPixPayload: ride.paymentPixPayload,
      paymentTxId: ride.paymentTxId,
    };
  }

  async startRide(userId: string, rideId: string) {
    const ride = await this.prisma.ride.findFirst({
      where: { id: rideId, driverId: userId },
      include: { passenger: true },
    });

    if (!ride) throw new NotFoundException('Corrida não encontrada');
    if (ride.status !== RideStatus.ACCEPTED && ride.status !== RideStatus.DRIVER_ARRIVED) {
      throw new BadRequestException('A corrida ainda não pode ser iniciada');
    }

    const updatedRide = await this.prisma.ride.update({
      where: { id: rideId },
      data: { status: RideStatus.IN_PROGRESS, startedAt: new Date() },
      include: { passenger: true },
    });

    this.realtime.emitRide(rideId, 'ride_status_update', {
      rideId,
      status: 'IN_PROGRESS',
    });
    this.notification.sendToUser(
      updatedRide.passengerId,
      'Corrida iniciada!',
      'Sua corrida começou. Boa viagem!',
      { rideId, event: 'IN_PROGRESS' },
    );

    return {
      success: true,
      message: 'Corrida iniciada com sucesso',
      rideId: updatedRide.id,
      passengerId: updatedRide.passengerId,
      passengerName: updatedRide.passenger?.name,
      passengerPhone: updatedRide.passenger?.phone,
      passengerPhotoUrl: updatedRide.passenger?.profilePhotoUrl ?? null,
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

  async markArriving(userId: string, rideId: string) {
    const ride = await this.prisma.ride.findFirst({ where: { id: rideId, driverId: userId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');
    if (ride.status !== RideStatus.ACCEPTED) {
      throw new BadRequestException('Corrida não está no status correto para esta ação');
    }
    const updated = await this.prisma.ride.update({
      where: { id: rideId },
      data: { status: RideStatus.DRIVER_ARRIVING, driverArrivingAt: new Date() },
      include: { passenger: true },
    });
    this.realtime.emitRide(rideId, 'ride_status_update', { rideId, status: 'DRIVER_ARRIVING' });
    this.notification.sendToUser(
      updated.passengerId,
      'Motorista chegando!',
      'Seu motorista está chegando ao ponto de embarque.',
      { rideId, event: 'DRIVER_ARRIVING' },
    );
    return {
      rideId: updated.id, passengerId: updated.passengerId,
      passengerName: updated.passenger?.name, passengerPhone: updated.passenger?.phone,
      passengerPhotoUrl: updated.passenger?.profilePhotoUrl ?? null,
      originLat: updated.originLat, originLng: updated.originLng,
      destLat: updated.destLat, destLng: updated.destLng,
      originAddress: updated.originAddress, destinationAddress: updated.destinationAddress,
      price: updated.estimatedFareCents, distanceMeters: updated.distanceMeters,
      durationSeconds: updated.durationSeconds, status: updated.status,
    };
  }

  async markArrived(userId: string, rideId: string) {
    const ride = await this.prisma.ride.findFirst({ where: { id: rideId, driverId: userId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');
    if (ride.status !== RideStatus.DRIVER_ARRIVING) {
      throw new BadRequestException('Corrida não está no status correto para esta ação');
    }
    const updated = await this.prisma.ride.update({
      where: { id: rideId },
      data: { status: RideStatus.DRIVER_ARRIVED, driverArrivedAt: new Date() },
      include: { passenger: true },
    });
    this.realtime.emitRide(rideId, 'ride_status_update', { rideId, status: 'DRIVER_ARRIVED' });
    this.notification.sendToUser(
      updated.passengerId,
      'Motorista chegou!',
      'Seu motorista chegou ao ponto de embarque. Dirija-se ao veículo.',
      { rideId, event: 'DRIVER_ARRIVED' },
    );
    return {
      rideId: updated.id, passengerId: updated.passengerId,
      passengerName: updated.passenger?.name, passengerPhone: updated.passenger?.phone,
      passengerPhotoUrl: updated.passenger?.profilePhotoUrl ?? null,
      originLat: updated.originLat, originLng: updated.originLng,
      destLat: updated.destLat, destLng: updated.destLng,
      originAddress: updated.originAddress, destinationAddress: updated.destinationAddress,
      price: updated.estimatedFareCents, distanceMeters: updated.distanceMeters,
      durationSeconds: updated.durationSeconds, status: updated.status,
    };
  }

  async updateProfile(userId: string, dto: UpdateDriverProfileDto) {
    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    const cleanString = (value?: string) => {
      if (value === undefined) return undefined;
      const trimmed = value.trim();
      return trimmed.length > 0 ? trimmed : null;
    };

    const name = cleanString(dto.name);
    if (name) {
      await this.prisma.user.update({
        where: { id: userId },
        data: { name },
      });
    }

    const updated = await this.prisma.driverProfile.update({
      where: { userId },
      data: {
        ...(dto.cnhNumber !== undefined && { cnhNumber: cleanString(dto.cnhNumber) }),
        ...(dto.cnhCategory !== undefined && { cnhCategory: cleanString(dto.cnhCategory)?.toUpperCase() ?? null }),
        ...(dto.hasEar !== undefined && { hasEar: dto.hasEar }),
        ...(dto.pixQrPayload !== undefined && { pixQrPayload: cleanString(dto.pixQrPayload) }),
        ...(dto.pixKey !== undefined && { pixKey: cleanString(dto.pixKey) }),
        ...(dto.pixQrCodeUrl !== undefined && { pixQrCodeUrl: cleanString(dto.pixQrCodeUrl) }),
        ...(dto.vehicleModel !== undefined && { vehicleModel: cleanString(dto.vehicleModel) }),
        ...(dto.vehiclePlate !== undefined && { vehiclePlate: cleanString(dto.vehiclePlate)?.toUpperCase() ?? null }),
        ...(dto.vehicleColor !== undefined && { vehicleColor: cleanString(dto.vehicleColor) }),
        ...(dto.cpf !== undefined && { cpf: cleanString(dto.cpf) }),
        ...(dto.vehicleYear !== undefined && { vehicleYear: dto.vehicleYear }),
        ...(dto.cnhExpiresAt !== undefined && { cnhExpiresAt: dto.cnhExpiresAt ? new Date(dto.cnhExpiresAt) : null }),
        ...(dto.vehicleCapacity !== undefined && { vehicleCapacity: dto.vehicleCapacity }),
      },
      include: { user: true },
    });

    return {
      id: updated.id,
      name: updated.user.name,
      phone: updated.user.phone,
      profilePhotoUrl: updated.profilePhotoUrl ?? null,
      cnhNumber: updated.cnhNumber,
      cnhCategory: updated.cnhCategory,
      cnhExpiresAt: updated.cnhExpiresAt?.toISOString().substring(0, 10) ?? null,
      hasEar: updated.hasEar,
      cpf: updated.cpf ?? null,
      pixKey: updated.pixKey,
      pixQrCodeUrl: updated.pixQrCodeUrl,
      pixQrPayload: updated.pixQrPayload,
      vehicleModel: updated.vehicleModel,
      vehiclePlate: updated.vehiclePlate,
      vehicleColor: updated.vehicleColor,
      vehicleYear: updated.vehicleYear ?? null,
      vehicleCapacity: updated.vehicleCapacity ?? null,
      approvalStatus: updated.approvalStatus,
    };
  }

  async cancelRide(userId: string, rideId: string) {
    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    const ride = await this.prisma.ride.findFirst({ where: { id: rideId, driverId: userId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');

    const cancelableStatuses = [
      RideStatus.ACCEPTED,
      RideStatus.DRIVER_ARRIVING,
      RideStatus.DRIVER_ARRIVED,
    ];
    if (!cancelableStatuses.includes(ride.status as any)) {
      throw new BadRequestException('Corrida não pode ser cancelada neste estágio');
    }

    await this.prisma.ride.update({
      where: { id: rideId },
      data: { status: RideStatus.PENDING_DRIVER, driverId: null },
    });

    await this.prisma.driverProfile.update({
      where: { userId },
      data: { available: true },
    });

    this.realtime.emitRide(rideId, 'ride_status_update', {
      rideId,
      status: 'PENDING_DRIVER',
      reason: 'driver_canceled',
    });
    this.notification.sendToUser(
      ride.passengerId,
      'Motorista cancelou a corrida',
      'Seu motorista cancelou. Estamos buscando outro para você.',
      { rideId, event: 'DRIVER_CANCELED' },
    );

    return { success: true, message: 'Corrida devolvida para fila de motoristas' };
  }

  async getRideHistory(userId: string) {
    const driver = await this.prisma.driverProfile.findUnique({ where: { userId } });
    if (!driver) throw new NotFoundException('Driver profile não encontrado');

    const rides = await this.prisma.ride.findMany({
      where: {
        driverId: userId,
        status: { in: [RideStatus.FINISHED, RideStatus.CANCELED] },
      },
      orderBy: { createdAt: 'desc' },
      take: 200,
      include: {
        passenger: { select: { name: true, phone: true } },
        review: { select: { rating: true, comment: true } },
      },
    });

    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const weekStart = new Date(todayStart);
    weekStart.setDate(todayStart.getDate() - todayStart.getDay());
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);

    const agg = (where: object) =>
      this.prisma.ride.aggregate({ where, _sum: { estimatedFareCents: true, driverReceivableCents: true, platformFeeCents: true } });

    const baseWhere = { driverId: userId, status: RideStatus.FINISHED };

    const [todayAgg, weekAgg, monthAgg, acceptedCount, rejectedCount, todayCount] = await Promise.all([
      agg({ ...baseWhere, createdAt: { gte: todayStart } }),
      agg({ ...baseWhere, createdAt: { gte: weekStart } }),
      agg({ ...baseWhere, createdAt: { gte: monthStart } }),
      this.prisma.ride.count({ where: { driverId: userId } }),
      this.prisma.rideRejection.count({ where: { driverId: driver.id } }),
      this.prisma.ride.count({ where: { ...baseWhere, createdAt: { gte: todayStart } } }),
    ]);

    const todayEarned = todayAgg._sum.estimatedFareCents ?? 0;
    const todayReceivable = todayAgg._sum.driverReceivableCents ?? todayEarned;
    const weekEarned = weekAgg._sum.estimatedFareCents ?? 0;
    const weekReceivable = weekAgg._sum.driverReceivableCents ?? weekEarned;
    const monthEarned = monthAgg._sum.estimatedFareCents ?? 0;
    const monthReceivable = monthAgg._sum.driverReceivableCents ?? monthEarned;

    const finishedRides = rides.filter((r) => r.status === RideStatus.FINISHED);
    const totalEarned = finishedRides.reduce((sum, r) => sum + (r.estimatedFareCents ?? 0), 0);
    const totalReceivable = finishedRides.reduce((sum, r) => sum + (r.driverReceivableCents ?? r.estimatedFareCents ?? 0), 0);
    const totalPlatformFee = finishedRides.reduce((sum, r) => sum + (r.platformFeeCents ?? 0), 0);

    return {
      totalEarned,
      totalReceivable,
      totalPlatformFee,
      todayEarned,
      todayReceivable,
      weekEarned,
      weekReceivable,
      monthEarned,
      monthReceivable,
      acceptedCount,
      rejectedCount,
      todayCount,
      rides: rides.map((r) => ({
        id: r.id,
        status: r.status,
        originAddress: r.originAddress,
        destinationAddress: r.destinationAddress,
        estimatedFareCents: r.estimatedFareCents,
        platformFeeCents: r.platformFeeCents ?? null,
        driverReceivableCents: r.driverReceivableCents ?? null,
        distanceMeters: r.distanceMeters,
        durationSeconds: r.durationSeconds,
        passengerName: r.passenger?.name ?? null,
        passengerPhone: r.passenger?.phone ?? null,
        ratingScore: r.review?.rating ?? null,
        ratingComment: r.review?.comment ?? null,
        createdAt: r.createdAt,
      })),
    };
  }

  async getAvailableDrivers() {
    const drivers = await this.prisma.driverProfile.findMany({
      where: {
        online: true,
        available: true,
        currentLat: { not: null },
        currentLng: { not: null },
      },
      select: { id: true, currentLat: true, currentLng: true },
    });
    return drivers.map((d) => ({
      id: d.id,
      lat: d.currentLat as number,
      lng: d.currentLng as number,
    }));
  }

  async finishRide(userId: string, rideId: string) {
    const ride = await this.prisma.ride.findFirst({
      where: { id: rideId, driverId: userId },
      include: {
        passenger: true,
        driver: { include: { driverProfile: true } },
        pricingConfig: true,
      },
    });

    if (!ride) throw new NotFoundException('Corrida não encontrada');
    if (ride.status !== RideStatus.IN_PROGRESS) {
      throw new BadRequestException('A corrida ainda não pode ser finalizada');
    }

    const fare = ride.estimatedFareCents ?? 0;
    const feePercent = ride.pricingConfig?.platformFeePercent ?? 20.0;
    const platformFeeCents = Math.round((fare * feePercent) / 100);
    const driverReceivableCents = fare - platformFeeCents;

    const txId = ride.paymentTxId ?? makePixTxId(ride.id);
    const generatedPixPayload =
      ride.paymentMethod === PaymentMethod.PIX && ride.driver?.driverProfile?.pixKey
        ? buildPixPayload({
            pixKey: ride.driver.driverProfile.pixKey,
            amountCents: fare,
            txId,
            merchantName: ride.driver.name,
            merchantCity: 'CONCEICAO',
          })
        : null;

    const paymentPixPayload =
      generatedPixPayload ??
      (ride.paymentMethod === PaymentMethod.PIX ? ride.driver?.driverProfile?.pixQrPayload ?? null : null);

    const updatedRide = await this.prisma.ride.update({
      where: { id: rideId },
      data: {
        status: RideStatus.FINISHED,
        finishedAt: new Date(),
        platformFeeCents,
        driverReceivableCents,
        paymentTxId: ride.paymentMethod === PaymentMethod.PIX ? txId : ride.paymentTxId,
        paymentPixPayload,
      },
      include: {
        passenger: true,
        driver: { include: { driverProfile: true } },
      },
    });

    // Motorista volta a ficar disponível para novas corridas
    await this.prisma.driverProfile.update({
      where: { userId },
      data: { available: true },
    });

    this.realtime.emitRide(rideId, 'ride_status_update', {
      rideId,
      status: 'FINISHED',
    });
    this.notification.sendToUser(
      updatedRide.passengerId,
      'Corrida finalizada!',
      'Você chegou ao destino. Obrigado por usar o MobU!',
      { rideId, event: 'FINISHED' },
    );

    return {
      success: true,
      message: 'Corrida finalizada com sucesso',
      rideId: updatedRide.id,
      passengerId: updatedRide.passengerId,
      passengerName: updatedRide.passenger?.name,
      passengerPhone: updatedRide.passenger?.phone,
      passengerPhotoUrl: updatedRide.passenger?.profilePhotoUrl ?? null,
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
      driverPixQrPayload: paymentPixPayload ?? updatedRide.driver?.driverProfile?.pixQrPayload ?? null,
      paymentMethod: updatedRide.paymentMethod,
      paymentStatus: updatedRide.paymentStatus ?? PaymentStatus.PENDING,
      paymentPixPayload: updatedRide.paymentPixPayload,
      paymentTxId: updatedRide.paymentTxId,
    };
  }

  private async getDebtBalance(userId: string): Promise<number> {
    const [feesAgg, settledAgg] = await Promise.all([
      this.prisma.ride.aggregate({
        where: { driverId: userId, status: RideStatus.FINISHED },
        _sum: { platformFeeCents: true },
      }),
      this.prisma.driverSettlement.aggregate({
        where: { driverId: userId },
        _sum: { amountCents: true },
      }),
    ]);
    return Math.max(0, (feesAgg._sum.platformFeeCents ?? 0) - (settledAgg._sum.amountCents ?? 0));
  }

  async getDriverBalance(userId: string) {
    const [feesAgg, settledAgg, settlements, paymentRequests, limitConfig, pixConfig] = await Promise.all([
      this.prisma.ride.aggregate({
        where: { driverId: userId, status: RideStatus.FINISHED },
        _sum: { platformFeeCents: true },
      }),
      this.prisma.driverSettlement.aggregate({
        where: { driverId: userId },
        _sum: { amountCents: true },
      }),
      this.prisma.driverSettlement.findMany({
        where: { driverId: userId },
        orderBy: { settledAt: 'desc' },
        take: 20,
        select: { id: true, amountCents: true, notes: true, method: true, settledAt: true },
      }),
      this.prisma.driverPaymentRequest.findMany({
        where: { driverId: userId },
        orderBy: { requestedAt: 'desc' },
        take: 10,
        select: { id: true, amountCents: true, status: true, notes: true, requestedAt: true, rejectionReason: true },
      }),
      this.prisma.systemConfig.findUnique({ where: { key: 'DRIVER_DEBT_LIMIT_CENTS' } }),
      this.prisma.systemConfig.findUnique({ where: { key: 'PLATFORM_PIX_KEY' } }),
    ]);

    const totalFeeCents = feesAgg._sum.platformFeeCents ?? 0;
    const totalSettledCents = settledAgg._sum.amountCents ?? 0;
    const balanceCents = Math.max(0, totalFeeCents - totalSettledCents);
    const limitCents = parseInt(limitConfig?.value ?? '5000', 10);
    const isBlocked = balanceCents > limitCents;
    const platformPixKey = pixConfig?.value ?? null;

    return {
      totalFeeCents,
      totalSettledCents,
      balanceCents,
      limitCents,
      isBlocked,
      platformPixKey,
      settlements,
      paymentRequests,
    };
  }

  async createPaymentRequest(userId: string, amountCents: number, notes: string | null) {
    if (amountCents <= 0) throw new BadRequestException('Valor deve ser positivo');

    const existing = await this.prisma.driverPaymentRequest.findFirst({
      where: { driverId: userId, status: PaymentRequestStatus.PENDING },
    });
    if (existing) throw new BadRequestException('Você já tem uma solicitação de pagamento pendente. Aguarde a confirmação do administrador.');

    return this.prisma.driverPaymentRequest.create({
      data: { driverId: userId, amountCents, notes },
      select: { id: true, amountCents: true, status: true, notes: true, requestedAt: true },
    });
  }

  async getPaymentRequests(userId: string) {
    return this.prisma.driverPaymentRequest.findMany({
      where: { driverId: userId },
      orderBy: { requestedAt: 'desc' },
      take: 20,
      select: { id: true, amountCents: true, status: true, notes: true, receiptUrl: true, requestedAt: true, rejectionReason: true, reviewedAt: true },
    });
  }

  async uploadPaymentRequestReceipt(userId: string, requestId: string, receiptUrl: string) {
    const req = await this.prisma.driverPaymentRequest.findFirst({
      where: { id: requestId, driverId: userId },
    });
    if (!req) throw new NotFoundException('Solicitação não encontrada');
    return this.prisma.driverPaymentRequest.update({
      where: { id: requestId },
      data: { receiptUrl },
      select: { id: true, receiptUrl: true },
    });
  }

  // ── Billing cycles ─────────────────────────────────────────────────────────

  private getWeekBounds(date: Date): { start: Date; end: Date } {
    const d = new Date(date);
    const day = d.getUTCDay(); // 0=Sun, 1=Mon … 6=Sat
    const diffToMonday = day === 0 ? -6 : 1 - day;
    const start = new Date(d);
    start.setUTCDate(d.getUTCDate() + diffToMonday);
    start.setUTCHours(0, 0, 0, 0);
    const end = new Date(start);
    end.setUTCDate(start.getUTCDate() + 6);
    end.setUTCHours(23, 59, 59, 999);
    return { start, end };
  }

  async getBillingCycles(userId: string) {
    const now = new Date();
    const { start: currentWeekStart } = this.getWeekBounds(now);

    // Look back 12 weeks
    const lookbackStart = new Date(currentWeekStart);
    lookbackStart.setUTCDate(lookbackStart.getUTCDate() - 11 * 7);

    const [rides, settledAgg, pixConfig, pendingRequest] = await Promise.all([
      this.prisma.ride.findMany({
        where: {
          driverId: userId,
          status: RideStatus.FINISHED,
          finishedAt: { gte: lookbackStart },
          platformFeeCents: { not: null },
        },
        select: {
          finishedAt: true,
          platformFeeCents: true,
          estimatedFareCents: true,
          driverReceivableCents: true,
          distanceMeters: true,
        },
        orderBy: { finishedAt: 'asc' },
      }),
      this.prisma.driverSettlement.aggregate({
        where: { driverId: userId },
        _sum: { amountCents: true },
      }),
      this.prisma.systemConfig.findUnique({ where: { key: 'PLATFORM_PIX_KEY' } }),
      this.prisma.driverPaymentRequest.findFirst({
        where: { driverId: userId, status: PaymentRequestStatus.PENDING },
        select: { id: true, amountCents: true, requestedAt: true },
      }),
    ]);

    // Group rides by ISO week
    type WeekData = {
      weekStart: Date; weekEnd: Date;
      totalFeeCents: number; totalGrossCents: number;
      totalReceivableCents: number; totalDistanceMeters: number;
      rideCount: number;
    };
    const weekMap = new Map<string, WeekData>();

    for (const ride of rides) {
      const { start, end } = this.getWeekBounds(ride.finishedAt!);
      const key = start.toISOString();
      if (!weekMap.has(key)) {
        weekMap.set(key, {
          weekStart: start, weekEnd: end,
          totalFeeCents: 0, totalGrossCents: 0,
          totalReceivableCents: 0, totalDistanceMeters: 0,
          rideCount: 0,
        });
      }
      const w = weekMap.get(key)!;
      w.totalFeeCents += ride.platformFeeCents ?? 0;
      w.totalGrossCents += ride.estimatedFareCents ?? 0;
      w.totalReceivableCents += ride.driverReceivableCents ?? 0;
      w.totalDistanceMeters += ride.distanceMeters ?? 0;
      w.rideCount++;
    }

    // Ensure current week exists even if no rides yet
    const currentKey = currentWeekStart.toISOString();
    if (!weekMap.has(currentKey)) {
      const { start, end } = this.getWeekBounds(now);
      weekMap.set(currentKey, {
        weekStart: start, weekEnd: end,
        totalFeeCents: 0, totalGrossCents: 0,
        totalReceivableCents: 0, totalDistanceMeters: 0,
        rideCount: 0,
      });
    }

    const totalSettledCents = settledAgg._sum.amountCents ?? 0;

    // FIFO credit: oldest cycles get credited first
    const sortedWeeks = Array.from(weekMap.values()).sort((a, b) => a.weekStart.getTime() - b.weekStart.getTime());
    let remainingSettled = totalSettledCents;

    const cycles = sortedWeeks.map(week => {
      const isCurrentWeek = week.weekStart.getTime() === currentWeekStart.getTime();

      if (isCurrentWeek) {
        const daysElapsed = Math.min(7, Math.floor((now.getTime() - week.weekStart.getTime()) / 86_400_000) + 1);
        // Apply any remaining settlement credit to the current week too
        const paidCents = Math.min(remainingSettled, week.totalFeeCents);
        remainingSettled = Math.max(0, remainingSettled - paidCents);
        return {
          ...week,
          weekStart: week.weekStart.toISOString(),
          weekEnd: week.weekEnd.toISOString(),
          status: 'OPEN' as const,
          paidCents,
          balanceCents: Math.max(0, week.totalFeeCents - paidCents),
          isCurrentWeek: true,
          daysElapsed,
          daysTotal: 7,
        };
      }

      let paidCents = 0;
      let status: 'PAID' | 'PARTIAL' | 'PENDING_PAYMENT' | 'OVERDUE';

      if (week.totalFeeCents === 0) {
        // No fees this week — mark as paid automatically
        status = 'PAID';
        paidCents = 0;
      } else if (remainingSettled >= week.totalFeeCents) {
        paidCents = week.totalFeeCents;
        remainingSettled -= week.totalFeeCents;
        status = 'PAID';
      } else if (remainingSettled > 0) {
        paidCents = remainingSettled;
        remainingSettled = 0;
        status = 'PARTIAL';
      } else {
        // Past due if more than 7 days after week end
        const overdueThreshold = new Date(week.weekEnd);
        overdueThreshold.setUTCDate(overdueThreshold.getUTCDate() + 7);
        status = now > overdueThreshold ? 'OVERDUE' : 'PENDING_PAYMENT';
      }

      return {
        ...week,
        weekStart: week.weekStart.toISOString(),
        weekEnd: week.weekEnd.toISOString(),
        status,
        paidCents,
        balanceCents: Math.max(0, week.totalFeeCents - paidCents),
        isCurrentWeek: false,
        daysElapsed: 7,
        daysTotal: 7,
      };
    });

    const pendingCount = cycles.filter(
      c => c.status === 'PENDING_PAYMENT' || c.status === 'OVERDUE' || c.status === 'PARTIAL',
    ).length;

    return {
      cycles: cycles.reverse(),
      currentWeekStart: currentWeekStart.toISOString(),
      platformPixKey: pixConfig?.value ?? null,
      totalFeeCents: sortedWeeks.reduce((s, w) => s + w.totalFeeCents, 0),
      totalSettledCents,
      pendingCount,
      hasPendingRequest: !!pendingRequest,
      pendingRequest: pendingRequest ?? null,
    };
  }
}
