import {
  ForbiddenException,
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { PaymentMethod, PaymentStatus, RideStatus } from '@prisma/client';
import type { Prisma } from '@prisma/client';
import { haversineMeters } from '../common/utils/geo.util';
import { ConfigService } from '@nestjs/config';
import { GoogleDirectionsService } from '../maps/google-directions.service';
import { RealtimeService } from '../realtime/realtime.service';
import { ConfirmPaymentDto } from '../payment/dto/confirm-payment.dto';
import { NotificationService } from '../notification/notification.service';

@Injectable()
export class RideService {
  constructor(
    private prisma: PrismaService,
    private config: ConfigService,
    private directions: GoogleDirectionsService,
    private realtime: RealtimeService,
    private notification: NotificationService,
  ) {}

  /**
   * Throws BadRequestException when active OperationRegions exist and the
   * given point falls outside every one of them.
   */
  private async assertWithinOperationRegion(lat: number, lng: number) {
    const regions = await this.prisma.operationRegion.findMany({
      where: {
        active: true,
        centerLat: { not: null },
        centerLng: { not: null },
        radiusMeters: { not: null },
      },
    });

    if (regions.length === 0) return; // no active region configured — allow all

    const inside = regions.some(
      (r) =>
        haversineMeters(lat, lng, r.centerLat!, r.centerLng!) <=
        r.radiusMeters!,
    );

    if (!inside) {
      throw new BadRequestException(
        'O ponto de origem está fora da área de operação configurada.',
      );
    }
  }

  private async nearestAvailableDrivers(
    originLat: number,
    originLng: number,
    limit = 5,
  ) {
    const drivers = await this.prisma.driverProfile.findMany({
      where: {
        online: true,
        available: true,
        approvalStatus: 'APPROVED',
        currentLat: { not: null },
        currentLng: { not: null },
        user: { blocked: false },
      },
      select: {
        userId: true,
        currentLat: true,
        currentLng: true,
      },
    });

    return drivers
      .map((driver) => ({
        ...driver,
        pickupDistanceMeters: haversineMeters(
          originLat,
          originLng,
          driver.currentLat!,
          driver.currentLng!,
        ),
      }))
      .sort((a, b) => a.pickupDistanceMeters - b.pickupDistanceMeters)
      .slice(0, limit);
  }

  private calcFareCents(params: {
    distanceMeters: number;
    durationSeconds: number;
    baseFareCents: number;
    perKmCents: number;
    perMinuteCents: number;
    minimumFareCents: number;
    bookingFeeCents: number;
    surgeMultiplier: number;
  }) {
    const km = params.distanceMeters / 1000;
    const minutes = params.durationSeconds / 60;
    const variable = params.perKmCents * km + params.perMinuteCents * minutes;
    const raw = params.baseFareCents + params.bookingFeeCents + variable;
    const surged = raw * (params.surgeMultiplier ?? 1);
    return Math.max(params.minimumFareCents, Math.round(surged));
  }

  async validateCoupon(code: string) {
    const promo = await this.prisma.promoCode.findFirst({
      where: { code: { equals: code, mode: 'insensitive' } },
    });

    if (!promo) {
      throw new BadRequestException('Cupom não encontrado');
    }
    if (!promo.active) {
      throw new BadRequestException('Cupom inativo');
    }
    if (promo.maxUses !== null && promo.usedCount >= promo.maxUses) {
      throw new BadRequestException('Cupom esgotado');
    }
    if (promo.expiresAt && promo.expiresAt < new Date()) {
      throw new BadRequestException('Cupom expirado');
    }

    return {
      valid: true,
      code: promo.code,
      discountPercent: promo.discountPercent ?? null,
      discountCents: promo.discountCents ?? null,
    };
  }

  async create(data: {
    passengerId: string;
    originLat: number;
    originLng: number;
    destLat: number;
    destLng: number;
    originAddress?: string;
    destinationAddress?: string;
    paymentMethod?: string;
    promoCode?: string;
  }) {
    await this.assertWithinOperationRegion(data.originLat, data.originLng);

    const apiKey = this.config.get<string>('GOOGLE_MAPS_API_KEY');
    if (!apiKey) {
      throw new BadRequestException('GOOGLE_MAPS_API_KEY não configurada no .env');
    }

    const { distanceMeters, durationSeconds } = await this.directions.getRoute(
      data.originLat,
      data.originLng,
      data.destLat,
      data.destLng,
      apiKey,
    );

    const pricing = await this.prisma.pricingConfig.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: 'desc' },
    });

    if (!pricing) {
      throw new BadRequestException('Não existe PricingConfig ativo. Rode o seed ou crie um no painel.');
    }

    let estimatedFareCents = this.calcFareCents({
      distanceMeters,
      durationSeconds,
      baseFareCents: pricing.baseFareCents,
      perKmCents: pricing.perKmCents,
      perMinuteCents: pricing.perMinuteCents,
      minimumFareCents: pricing.minimumFareCents,
      bookingFeeCents: pricing.bookingFeeCents,
      surgeMultiplier: pricing.surgeMultiplier,
    });

    // [KL-01 fix] Cupom: lê os valores de desconto e tenta incrementar atomicamente.
    // O updateMany só executa se usedCount ainda estiver abaixo do limite — evita TOCTOU.
    if (data.promoCode) {
      const promo = await this.prisma.promoCode.findFirst({
        where: { code: { equals: data.promoCode, mode: 'insensitive' } },
      });

      if (promo) {
        const claimWhere: Prisma.PromoCodeWhereInput = { id: promo.id, active: true };
        if (promo.maxUses !== null) claimWhere.usedCount = { lt: promo.maxUses };
        if (promo.expiresAt !== null) claimWhere.expiresAt = { gte: new Date() };

        const claimed = await this.prisma.promoCode.updateMany({
          where: claimWhere,
          data: { usedCount: { increment: 1 } },
        });

        if (claimed.count > 0) {
          if (promo.discountPercent != null) {
            estimatedFareCents = Math.max(
              0,
              Math.round(estimatedFareCents * (1 - promo.discountPercent / 100)),
            );
          } else if (promo.discountCents != null) {
            estimatedFareCents = Math.max(0, estimatedFareCents - promo.discountCents);
          }
        }
      }
    }

    const paymentMethod =
      data.paymentMethod === PaymentMethod.PIX ? PaymentMethod.PIX : PaymentMethod.CASH;

    const ride = await this.prisma.ride.create({
      data: {
        passengerId: data.passengerId,
        driverId: null,
        status: RideStatus.PENDING_DRIVER,
        originLat: data.originLat,
        originLng: data.originLng,
        destLat: data.destLat,
        destLng: data.destLng,
        originAddress: data.originAddress ?? null,
        destinationAddress: data.destinationAddress ?? null,
        distanceMeters,
        durationSeconds,
        estimatedFareCents,
        pricingConfigId: pricing.id,
        paymentMethod,
      },
    });

    const nearbyDrivers = await this.nearestAvailableDrivers(
      ride.originLat,
      ride.originLng,
    );

    if (nearbyDrivers.length > 0) {
      const driverUserIds = nearbyDrivers.map((driver) => driver.userId);
      this.realtime.emitToDrivers(
        driverUserIds,
        'ride_offer',
        {
          rideId: ride.id,
          originAddress: ride.originAddress,
          destinationAddress: ride.destinationAddress,
          originLat: ride.originLat,
          originLng: ride.originLng,
          destLat: ride.destLat,
          destLng: ride.destLng,
          estimatedFareCents: ride.estimatedFareCents,
          distanceMeters: ride.distanceMeters,
          durationSeconds: ride.durationSeconds,
          paymentMethod: ride.paymentMethod,
          notifiedDrivers: nearbyDrivers.length,
        },
      );
      this.notification.sendToUsers(
        driverUserIds,
        'Nova corrida disponível!',
        `${ride.originAddress ?? 'Embarque'} → ${ride.destinationAddress ?? 'Destino'}`,
        { rideId: ride.id, event: 'NEW_RIDE_OFFER' },
      );
    }

    this.realtime.emitAdmin('ride_created', {
      rideId: ride.id,
      status: ride.status,
      notifiedDrivers: nearbyDrivers.length,
    });

    return ride;
  }

  async findOpen() {
    return this.prisma.ride.findMany({
      where: { status: RideStatus.PENDING_DRIVER },
      orderBy: { createdAt: 'desc' },
    });
  }

  // Passageiro acompanha status da corrida e localização do motorista
  async findById(rideId: string, userId: string, role: string) {
    const where =
      role === 'ADMIN' ? { id: rideId } : { id: rideId, passengerId: userId };

    const ride = await this.prisma.ride.findFirst({
      where,
      include: {
        driver: {
          include: { driverProfile: true },
        },
        review: true,
      },
    });

    if (!ride) throw new NotFoundException('Corrida não encontrada');

    const driverAvgRating = ride.driverId
      ? await this.prisma.review
          .aggregate({
            where: { driverId: ride.driverId },
            _avg: { rating: true },
          })
          .then((r) => (r._avg.rating != null ? Number(r._avg.rating.toFixed(1)) : null))
      : null;

    return {
      id: ride.id,
      status: ride.status,
      estimatedFareCents: ride.estimatedFareCents,
      distanceMeters: ride.distanceMeters,
      durationSeconds: ride.durationSeconds,
      originAddress: ride.originAddress,
      destinationAddress: ride.destinationAddress,
      paymentMethod: ride.paymentMethod,
      driver: ride.driver
        ? {
            name: ride.driver.name,
            phone: ride.driver.phone,
            photoUrl: ride.driver.driverProfile?.profilePhotoUrl ?? null,
            currentLat: ride.driver.driverProfile?.currentLat ?? null,
            currentLng: ride.driver.driverProfile?.currentLng ?? null,
            vehicleModel: ride.driver.driverProfile?.vehicleModel ?? null,
            vehiclePlate: ride.driver.driverProfile?.vehiclePlate ?? null,
            vehicleColor: ride.driver.driverProfile?.vehicleColor ?? null,
            averageRating: driverAvgRating,
            pixKey: ride.driver.driverProfile?.pixKey ?? null,
          }
        : null,
      paymentStatus: ride.paymentStatus,
      paymentPixPayload: ride.paymentPixPayload,
      paymentTxId: ride.paymentTxId,
      paymentConfirmedAt: ride.paymentConfirmedAt,
      hasRating: !!ride.review,
    };
  }

  // Histórico de corridas do passageiro
  async myRides(passengerId: string) {
    const rides = await this.prisma.ride.findMany({
      where: { passengerId },
      orderBy: { createdAt: 'desc' },
      take: 30,
      include: {
        driver: { select: { name: true } },
        review: { select: { rating: true } },
      },
    });

    return rides.map((r) => ({
      id: r.id,
      status: r.status,
      originAddress: r.originAddress,
      destinationAddress: r.destinationAddress,
      estimatedFareCents: r.estimatedFareCents,
      distanceMeters: r.distanceMeters,
      driverName: r.driver?.name ?? null,
      rating: r.review?.rating ?? null,
      createdAt: r.createdAt,
    }));
  }

  // Passageiro cancela, ou admin muda status
  async updateStatus(rideId: string, status: RideStatus, userId: string, role: string) {
    const ride = await this.prisma.ride.findUnique({ where: { id: rideId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');

    // Passageiro só pode cancelar a própria corrida
    if (role !== 'ADMIN') {
      if (ride.passengerId !== userId) {
        throw new ForbiddenException('Você não tem permissão para alterar esta corrida');
      }
      if (status !== RideStatus.CANCELED) {
        throw new ForbiddenException('Passageiro só pode cancelar a corrida');
      }
      if (
        ride.status !== RideStatus.PENDING_DRIVER &&
        ride.status !== RideStatus.ACCEPTED
      ) {
        throw new BadRequestException('Corrida não pode ser cancelada neste estágio');
      }
    }

    const updated = await this.prisma.ride.update({
      where: { id: rideId },
      data: { status },
    });

    // Notify passenger and driver in real-time
    this.realtime.emitRide(rideId, 'ride_status_update', {
      rideId,
      status,
    });

    return updated;
  }

  async estimate(data: {
    originLat: number;
    originLng: number;
    destLat: number;
    destLng: number;
  }) {
    const vals = [data.originLat, data.originLng, data.destLat, data.destLng];
    if (vals.some((v) => typeof v !== 'number' || Number.isNaN(v))) {
      throw new BadRequestException('Coordenadas inválidas');
    }

    await this.assertWithinOperationRegion(data.originLat, data.originLng);

    const apiKey = this.config.get<string>('GOOGLE_MAPS_API_KEY');
    if (!apiKey) {
      throw new BadRequestException('GOOGLE_MAPS_API_KEY não configurada no .env');
    }

    const { distanceMeters, durationSeconds, encodedPolyline } = await this.directions.getRoute(
      data.originLat,
      data.originLng,
      data.destLat,
      data.destLng,
      apiKey,
    );

    const pricing = await this.prisma.pricingConfig.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: 'desc' },
    });

    if (!pricing) {
      throw new BadRequestException('Não existe PricingConfig ativo.');
    }

    const estimatedFareCents = this.calcFareCents({
      distanceMeters,
      durationSeconds,
      baseFareCents: pricing.baseFareCents,
      perKmCents: pricing.perKmCents,
      perMinuteCents: pricing.perMinuteCents,
      minimumFareCents: pricing.minimumFareCents,
      bookingFeeCents: pricing.bookingFeeCents,
      surgeMultiplier: pricing.surgeMultiplier,
    });

    return {
      distanceMeters,
      durationSeconds,
      estimatedFareCents,
      encodedPolyline,
      currency: pricing.currency,
      pricingConfigId: pricing.id,
    };
  }

  async updatePaymentStatus(
    rideId: string,
    paymentStatus: PaymentStatus,
    user: { userId: string; role: string },
    dto?: { txId?: string; receiptNote?: string },
  ) {
    const ride = await this.prisma.ride.findUnique({ where: { id: rideId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');

    const isDriver = ride.driverId === user.userId;
    const isAdmin = user.role === 'ADMIN';

    if (!isDriver && !isAdmin) {
      throw new ForbiddenException('Apenas o motorista ou admin pode atualizar o pagamento');
    }

    if (ride.status === RideStatus.CANCELED && paymentStatus !== PaymentStatus.CANCELED) {
      throw new BadRequestException('Corrida cancelada deve manter pagamento cancelado');
    }

    // [KL-02 fix] Pagamento só pode ser registrado em corridas finalizadas
    if (
      (paymentStatus === PaymentStatus.RECEIVED || paymentStatus === PaymentStatus.NOT_RECEIVED) &&
      ride.status !== RideStatus.FINISHED
    ) {
      throw new BadRequestException('O pagamento só pode ser registrado em corridas finalizadas');
    }

    // [KL-03 fix] Confirmação idempotente: rejeita re-confirmação para preservar timestamp original
    if (paymentStatus === PaymentStatus.RECEIVED && ride.paymentStatus === PaymentStatus.RECEIVED) {
      throw new BadRequestException('Pagamento já confirmado');
    }

    const now = new Date();
    const txId = dto?.txId?.trim();
    const receiptNote = dto?.receiptNote?.trim();
    const data: Prisma.RideUpdateInput = {
      paymentStatus,
      ...(txId !== undefined && { paymentTxId: txId.length > 0 ? txId : null }),
      ...(receiptNote !== undefined && {
        paymentReceiptNote: receiptNote.length > 0 ? receiptNote : null,
      }),
    };

    if (paymentStatus === PaymentStatus.RECEIVED) {
      data.paymentReportedAt = now;
      data.paymentConfirmedAt = now;
      data.paymentConfirmedBy = user.userId;
    } else if (paymentStatus === PaymentStatus.NOT_RECEIVED) {
      data.paymentReportedAt = now;
      data.paymentConfirmedAt = null;
      data.paymentConfirmedBy = null;
    } else if (paymentStatus === PaymentStatus.CANCELED) {
      data.paymentReportedAt = now;
      data.paymentConfirmedAt = null;
      data.paymentConfirmedBy = null;
    } else {
      data.paymentReportedAt = null;
      data.paymentConfirmedAt = null;
      data.paymentConfirmedBy = null;
    }

    const updated = await this.prisma.ride.update({
      where: { id: rideId },
      data,
    });

    const payload = {
      rideId: updated.id,
      paymentStatus: updated.paymentStatus,
      paymentConfirmedAt: updated.paymentConfirmedAt,
      paymentTxId: updated.paymentTxId,
    };
    this.realtime.emitRide(rideId, 'payment_status_update', payload);
    this.realtime.emitAdmin('payment_status_update', payload);

    return updated;
  }

  async confirmPayment(
    rideId: string,
    user: { userId: string; role: string },
    dto: ConfirmPaymentDto = {},
  ) {
    const ride = await this.updatePaymentStatus(
      rideId,
      PaymentStatus.RECEIVED,
      user,
      dto,
    );

    return {
      ok: true,
      rideId: ride.id,
      paymentStatus: ride.paymentStatus,
      paymentConfirmedAt: ride.paymentConfirmedAt,
      paymentTxId: ride.paymentTxId,
    };
  }

  async markPaymentNotReceived(
    rideId: string,
    user: { userId: string; role: string },
    dto: Pick<ConfirmPaymentDto, 'receiptNote'> = {},
  ) {
    const ride = await this.updatePaymentStatus(
      rideId,
      PaymentStatus.NOT_RECEIVED,
      user,
      dto,
    );

    return {
      ok: true,
      rideId: ride.id,
      paymentStatus: ride.paymentStatus,
      paymentReportedAt: ride.paymentReportedAt,
    };
  }
}
