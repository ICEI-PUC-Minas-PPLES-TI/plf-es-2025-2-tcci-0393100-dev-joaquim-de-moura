import { BadRequestException, Injectable, NotFoundException, UnauthorizedException } from '@nestjs/common';
import {
  DriverApprovalStatus,
  PaymentRequestStatus,
  PaymentStatus,
  Prisma,
  RideStatus,
  SupportTicketStatus,
  UserRole,
} from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { NotificationService } from '../notification/notification.service';
import { CreatePricingConfigDto } from './dto/create-pricing-config.dto';
import { ListRidesQueryDto } from './dto/list-rides-query.dto';
import { ListReportsQueryDto } from './dto/list-reports-query.dto';
import {
  CreateOperationRegionDto,
  UpdateOperationRegionDto,
} from './dto/operation-region.dto';

@Injectable()
export class AdminService {
  constructor(
    private prisma: PrismaService,
    private notification: NotificationService,
  ) {}

  async listPendingDrivers() {
    return this.listDrivers(DriverApprovalStatus.PENDING);
  }

  async listDrivers(status?: DriverApprovalStatus) {
    const drivers = await this.prisma.driverProfile.findMany({
      where: {
        ...(status ? { approvalStatus: status } : {}),
      },
      include: {
        user: true,
      },
      orderBy: {
        createdAt: 'desc',
      },
    });

    const driverUserIds = drivers.map((d) => d.userId);

    const [rideCountsRaw, reviewAggRaw] = await Promise.all([
      this.prisma.ride.groupBy({
        by: ['driverId'],
        where: {
          driverId: { in: driverUserIds },
          status: RideStatus.FINISHED,
        },
        _count: { id: true },
      }),
      this.prisma.review.groupBy({
        by: ['driverId'],
        where: { driverId: { in: driverUserIds } },
        _avg: { rating: true },
        _count: { rating: true },
      }),
    ]);

    const rideCountMap = new Map(rideCountsRaw.map((r) => [r.driverId, r._count.id]));
    const reviewMap = new Map(
      reviewAggRaw.map((r) => [r.driverId, { avg: r._avg.rating, count: r._count.rating }]),
    );

    return drivers.map((d) => ({
      ...d,
      totalFinishedRides: rideCountMap.get(d.userId) ?? 0,
      averageRating: reviewMap.get(d.userId)?.avg ?? null,
      totalReviews: reviewMap.get(d.userId)?.count ?? 0,
    }));
  }

  async listPassengers() {
    const passengers = await this.prisma.user.findMany({
      where: { role: UserRole.PASSENGER },
      orderBy: { createdAt: 'desc' },
    });

    const passengerIds = passengers.map((p) => p.id);

    const [rideCountsRaw, ticketCountsRaw] = await Promise.all([
      this.prisma.ride.groupBy({
        by: ['passengerId'],
        where: { passengerId: { in: passengerIds } },
        _count: { id: true },
      }),
      this.prisma.supportTicket.groupBy({
        by: ['creatorId'],
        where: { creatorId: { in: passengerIds } },
        _count: { id: true },
      }),
    ]);

    const rideMap = new Map(rideCountsRaw.map((r) => [r.passengerId, r._count.id]));
    const ticketMap = new Map(ticketCountsRaw.map((t) => [t.creatorId, t._count.id]));

    return passengers.map((p) => ({
      ...p,
      totalRides: rideMap.get(p.id) ?? 0,
      totalTickets: ticketMap.get(p.id) ?? 0,
    }));
  }

  async blockUser(userId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new NotFoundException('Usuário não encontrado');
    if (user.role === UserRole.ADMIN) throw new BadRequestException('Não é possível bloquear um administrador');

    return this.prisma.$transaction(async (tx) => {
      if (user.role === UserRole.DRIVER) {
        await tx.driverProfile.updateMany({
          where: { userId },
          data: { online: false, available: false },
        });
      }

      return tx.user.update({
        where: { id: userId },
        data: { blocked: true, blockedAt: new Date() },
      });
    });
  }

  async unblockUser(userId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new NotFoundException('Usuário não encontrado');
    return this.prisma.user.update({
      where: { id: userId },
      data: { blocked: false, blockedAt: null },
    });
  }

  async approveDriver(driverProfileId: string) {
    const driver = await this.prisma.driverProfile.findUnique({
      where: { id: driverProfileId },
    });

    if (!driver) {
      throw new NotFoundException('Motorista não encontrado');
    }

    const updated = await this.prisma.driverProfile.update({
      where: { id: driverProfileId },
      data: {
        approvalStatus: DriverApprovalStatus.APPROVED,
        approvedAt: new Date(),
        rejectionReason: null,
      },
    });
    this.notification.sendToUser(
      updated.userId,
      'Cadastro aprovado!',
      'Parabéns! Seu cadastro foi aprovado. Você já pode ficar online e receber corridas.',
      { event: 'DRIVER_APPROVED' },
    );
    return updated;
  }

  async rejectDriver(driverProfileId: string, reason: string) {
    const rejectionReason = reason?.trim();
    const driver = await this.prisma.driverProfile.findUnique({
      where: { id: driverProfileId },
    });

    if (!driver) {
      throw new NotFoundException('Motorista não encontrado');
    }

    if (!rejectionReason) {
      throw new BadRequestException('Motivo da rejeição é obrigatório');
    }

    const updated = await this.prisma.driverProfile.update({
      where: { id: driverProfileId },
      data: {
        approvalStatus: DriverApprovalStatus.REJECTED,
        approvedAt: null,
        rejectionReason,
        online: false,
        available: false,
      },
    });
    this.notification.sendToUser(
      updated.userId,
      'Cadastro não aprovado',
      `Seu cadastro foi recusado. Motivo: ${rejectionReason}`,
      { event: 'DRIVER_REJECTED' },
    );
    return updated;
  }

  async listRides(query: ListRidesQueryDto = {}) {
    return this.prisma.ride.findMany({
      where: {
        ...(query.status ? { status: query.status } : {}),
        ...(query.paymentStatus ? { paymentStatus: query.paymentStatus } : {}),
        ...(query.driverId ? { driverId: query.driverId } : {}),
        ...(query.passengerId ? { passengerId: query.passengerId } : {}),
      },
      include: {
        passenger: { select: { id: true, name: true, phone: true } },
        driver: { select: { id: true, name: true, phone: true } },
        review: true,
      },
      orderBy: {
        createdAt: 'desc',
      },
      take: 100,
    });
  }

  async stats(dateFilter?: { gte?: Date; lte?: Date }) {
    const df = dateFilter ?? {};
    const rideWhere = Object.keys(df).length ? { createdAt: df } : {};
    const [
      totalRides,
      finishedRides,
      canceledRides,
      pendingPayments,
      receivedPayments,
      notReceivedPayments,
      openSupportTickets,
      onlineDrivers,
      availableDrivers,
      receivedRevenue,
    ] = await Promise.all([
      this.prisma.ride.count({ where: rideWhere }),
      this.prisma.ride.count({ where: { ...rideWhere, status: RideStatus.FINISHED } }),
      this.prisma.ride.count({ where: { ...rideWhere, status: RideStatus.CANCELED } }),
      this.prisma.ride.count({ where: { ...rideWhere, paymentStatus: PaymentStatus.PENDING } }),
      this.prisma.ride.count({ where: { ...rideWhere, paymentStatus: PaymentStatus.RECEIVED } }),
      this.prisma.ride.count({ where: { ...rideWhere, paymentStatus: PaymentStatus.NOT_RECEIVED } }),
      this.prisma.supportTicket.count({
        where: { status: { in: [SupportTicketStatus.OPEN, SupportTicketStatus.IN_REVIEW] } },
      }),
      this.prisma.driverProfile.count({ where: { online: true } }),
      this.prisma.driverProfile.count({ where: { online: true, available: true } }),
      this.prisma.ride.aggregate({
        where: { ...rideWhere, status: RideStatus.FINISHED, paymentStatus: PaymentStatus.RECEIVED },
        _sum: { estimatedFareCents: true },
      }),
    ]);

    return {
      totalRides,
      finishedRides,
      canceledRides,
      pendingPayments,
      receivedPayments,
      notReceivedPayments,
      openSupportTickets,
      onlineDrivers,
      availableDrivers,
      receivedRevenueCents: receivedRevenue._sum.estimatedFareCents ?? 0,
    };
  }

  async liveOperation() {
    const [activeRides, onlineDrivers] = await Promise.all([
      this.prisma.ride.findMany({
        where: {
          status: {
            in: [
              RideStatus.PENDING_DRIVER,
              RideStatus.ACCEPTED,
              RideStatus.DRIVER_ARRIVING,
              RideStatus.DRIVER_ARRIVED,
              RideStatus.IN_PROGRESS,
            ],
          },
        },
        include: {
          passenger: { select: { id: true, name: true, phone: true } },
          driver: {
            select: {
              id: true,
              name: true,
              phone: true,
              driverProfile: {
                select: {
                  currentLat: true,
                  currentLng: true,
                  online: true,
                  available: true,
                  vehicleModel: true,
                  vehiclePlate: true,
                  vehicleColor: true,
                  updatedAt: true,
                },
              },
            },
          },
        },
        orderBy: { updatedAt: 'desc' },
        take: 100,
      }),
      this.prisma.driverProfile.findMany({
        where: { online: true, approvalStatus: DriverApprovalStatus.APPROVED },
        include: { user: { select: { id: true, name: true, phone: true } } },
        orderBy: { updatedAt: 'desc' },
        take: 100,
      }),
    ]);

    return {
      activeRides,
      onlineDrivers: onlineDrivers.map((driver) => ({
        id: driver.id,
        userId: driver.userId,
        name: driver.user.name,
        phone: driver.user.phone,
        online: driver.online,
        available: driver.available,
        currentLat: driver.currentLat,
        currentLng: driver.currentLng,
        vehicleModel: driver.vehicleModel,
        vehiclePlate: driver.vehiclePlate,
        vehicleColor: driver.vehicleColor,
        locationUpdatedAt: driver.updatedAt,
      })),
      generatedAt: new Date().toISOString(),
    };
  }

  async reports(query: ListReportsQueryDto = {}) {
    const dateFilter =
      query.from || query.to
        ? { gte: query.from ? new Date(query.from) : undefined, lte: query.to ? new Date(query.to) : undefined }
        : undefined;
    const rideWhere = dateFilter ? { createdAt: dateFilter } : {};

    const [
      stats,
      ridesByStatus,
      paymentsByStatus,
      driversByApproval,
      rating,
      estimatedFinishedFare,
    ] = await Promise.all([
      this.stats(dateFilter),
      this.prisma.ride.groupBy({
        by: ['status'],
        where: rideWhere,
        _count: { status: true },
      }),
      this.prisma.ride.groupBy({
        by: ['paymentStatus'],
        where: rideWhere,
        _count: { paymentStatus: true },
      }),
      this.prisma.driverProfile.groupBy({
        by: ['approvalStatus'],
        _count: { approvalStatus: true },
      }),
      this.prisma.review.aggregate({
        where: dateFilter ? { createdAt: dateFilter } : {},
        _avg: { rating: true },
        _count: { rating: true },
      }),
      this.prisma.ride.aggregate({
        where: { ...rideWhere, status: RideStatus.FINISHED },
        _sum: { estimatedFareCents: true },
      }),
    ]);

    return {
      ...stats,
      estimatedFinishedFareCents: estimatedFinishedFare._sum.estimatedFareCents ?? 0,
      averageRating: rating._avg.rating ?? null,
      totalReviews: rating._count.rating,
      ridesByStatus: Object.fromEntries(
        ridesByStatus.map((item) => [item.status, item._count.status]),
      ),
      paymentsByStatus: Object.fromEntries(
        paymentsByStatus.map((item) => [item.paymentStatus, item._count.paymentStatus]),
      ),
      driversByApproval: Object.fromEntries(
        driversByApproval.map((item) => [item.approvalStatus, item._count.approvalStatus]),
      ),
      generatedAt: new Date(),
    };
  }

  async listPricingConfigs() {
    return this.prisma.pricingConfig.findMany({
      include: { region: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  async createPricingConfig(dto: CreatePricingConfigDto) {
    const isActive = dto.isActive ?? true;
    const regionId = dto.regionId?.trim() || null;

    if (regionId) {
      const region = await this.prisma.operationRegion.findUnique({
        where: { id: regionId },
      });

      if (!region) {
        throw new NotFoundException('Região operacional não encontrada');
      }
    }

    return this.prisma.$transaction(async (tx) => {
      if (isActive) {
        await tx.pricingConfig.updateMany({
          where: { regionId, isActive: true },
          data: { isActive: false },
        });
      }

      return tx.pricingConfig.create({
        data: this.pricingData(dto, isActive, regionId),
        include: { region: true },
      });
    });
  }

  async activatePricingConfig(pricingConfigId: string) {
    const pricing = await this.prisma.pricingConfig.findUnique({
      where: { id: pricingConfigId },
    });

    if (!pricing) {
      throw new NotFoundException('Configuração de tarifa não encontrada');
    }

    return this.prisma.$transaction(async (tx) => {
      await tx.pricingConfig.updateMany({
        where: { regionId: pricing.regionId, isActive: true },
        data: { isActive: false },
      });

      return tx.pricingConfig.update({
        where: { id: pricingConfigId },
        data: { isActive: true },
        include: { region: true },
      });
    });
  }

  async deactivatePricingConfig(pricingConfigId: string) {
    const pricing = await this.prisma.pricingConfig.findUnique({
      where: { id: pricingConfigId },
    });
    if (!pricing) throw new NotFoundException('Configuração de tarifa não encontrada');

    return this.prisma.pricingConfig.update({
      where: { id: pricingConfigId },
      data: { isActive: false },
      include: { region: true },
    });
  }

  async deletePricingConfig(pricingConfigId: string) {
    const pricing = await this.prisma.pricingConfig.findUnique({
      where: { id: pricingConfigId },
    });
    if (!pricing) throw new NotFoundException('Configuração de tarifa não encontrada');
    if (pricing.isActive)
      throw new BadRequestException('Desative a tarifa antes de excluí-la.');

    return this.prisma.pricingConfig.delete({ where: { id: pricingConfigId } });
  }

  async listRegions() {
    return this.prisma.operationRegion.findMany({
      include: {
        pricingConfigs: {
          where: { isActive: true },
          orderBy: { createdAt: 'desc' },
          take: 1,
        },
      },
      orderBy: [{ active: 'desc' }, { name: 'asc' }],
    });
  }

  async createRegion(dto: CreateOperationRegionDto) {
    const name = dto.name.trim();

    if (!name) {
      throw new BadRequestException('Nome da região é obrigatório');
    }

    return this.prisma.operationRegion.create({
      data: {
        name,
        city: dto.city?.trim() || null,
        active: dto.active ?? true,
        centerLat: dto.centerLat,
        centerLng: dto.centerLng,
        radiusMeters: dto.radiusMeters,
      },
    });
  }

  async updateRegion(regionId: string, dto: UpdateOperationRegionDto) {
    const region = await this.prisma.operationRegion.findUnique({
      where: { id: regionId },
    });

    if (!region) {
      throw new NotFoundException('Região operacional não encontrada');
    }

    if (dto.name !== undefined && !dto.name.trim()) {
      throw new BadRequestException('Nome da região é obrigatório');
    }

    return this.prisma.operationRegion.update({
      where: { id: regionId },
      data: {
        ...(dto.name !== undefined ? { name: dto.name.trim() } : {}),
        ...(dto.city !== undefined ? { city: dto.city.trim() || null } : {}),
        ...(dto.active !== undefined ? { active: dto.active } : {}),
        ...(dto.centerLat !== undefined ? { centerLat: dto.centerLat } : {}),
        ...(dto.centerLng !== undefined ? { centerLng: dto.centerLng } : {}),
        ...(dto.radiusMeters !== undefined ? { radiusMeters: dto.radiusMeters } : {}),
      },
    });
  }

  async deleteRegion(regionId: string) {
    const region = await this.prisma.operationRegion.findUnique({
      where: { id: regionId },
      include: { pricingConfigs: { where: { isActive: true } } },
    });

    if (!region) throw new NotFoundException('Região não encontrada');

    if (region.pricingConfigs.length > 0)
      throw new BadRequestException(
        'Desative todas as tarifas desta região antes de excluí-la.',
      );

    await this.prisma.pricingConfig.deleteMany({ where: { regionId } });
    return this.prisma.operationRegion.delete({ where: { id: regionId } });
  }

  async financialSummary() {
    const [rides, settlements, pendingRequestsCount] = await Promise.all([
      this.prisma.ride.findMany({
        where: { status: RideStatus.FINISHED },
        include: { driver: { select: { id: true, name: true, phone: true } } },
      }),
      this.prisma.driverSettlement.findMany({
        include: {
          driver: { select: { id: true, name: true } },
          settledByUser: { select: { name: true } },
        },
        orderBy: { settledAt: 'desc' },
      }),
      this.prisma.driverPaymentRequest.count({ where: { status: PaymentRequestStatus.PENDING } }),
    ]);

    const totalGrossCents = rides.reduce((s, r) => s + (r.estimatedFareCents ?? 0), 0);
    const totalPlatformFeeCents = rides.reduce((s, r) => s + (r.platformFeeCents ?? 0), 0);
    const totalDriverReceivableCents = rides.reduce((s, r) => s + (r.driverReceivableCents ?? 0), 0);
    const totalSettledCents = settlements.reduce((s, st) => s + st.amountCents, 0);

    const driverMap = new Map<string, {
      driverId: string; driverName: string | null; driverPhone: string | null;
      rides: number; grossCents: number; feeCents: number; receivableCents: number;
      settledCents: number; balanceCents: number;
    }>();

    for (const ride of rides) {
      if (!ride.driverId) continue;
      if (!driverMap.has(ride.driverId)) {
        driverMap.set(ride.driverId, {
          driverId: ride.driverId,
          driverName: ride.driver?.name ?? null,
          driverPhone: ride.driver?.phone ?? null,
          rides: 0, grossCents: 0, feeCents: 0, receivableCents: 0,
          settledCents: 0, balanceCents: 0,
        });
      }
      const e = driverMap.get(ride.driverId)!;
      e.rides += 1;
      e.grossCents += ride.estimatedFareCents ?? 0;
      e.feeCents += ride.platformFeeCents ?? 0;
      e.receivableCents += ride.driverReceivableCents ?? 0;
    }

    for (const st of settlements) {
      if (!driverMap.has(st.driverId)) {
        driverMap.set(st.driverId, {
          driverId: st.driverId, driverName: st.driver?.name ?? null, driverPhone: null,
          rides: 0, grossCents: 0, feeCents: 0, receivableCents: 0,
          settledCents: 0, balanceCents: 0,
        });
      }
      driverMap.get(st.driverId)!.settledCents += st.amountCents;
    }

    for (const e of driverMap.values()) {
      e.balanceCents = Math.max(0, e.feeCents - e.settledCents);
    }

    return {
      overview: {
        totalRides: rides.length,
        totalGrossCents,
        totalPlatformFeeCents,
        totalDriverReceivableCents,
        totalSettledCents,
        totalPendingCents: Math.max(0, totalPlatformFeeCents - totalSettledCents),
        pendingPaymentRequests: pendingRequestsCount,
      },
      byDriver: Array.from(driverMap.values()).sort((a, b) => b.balanceCents - a.balanceCents),
      recentSettlements: settlements.slice(0, 50).map((st) => ({
        id: st.id,
        driverId: st.driverId,
        driverName: st.driver?.name ?? null,
        amountCents: st.amountCents,
        notes: st.notes,
        method: st.method,
        settledAt: st.settledAt,
        settledBy: st.settledByUser?.name ?? 'Admin',
        paymentRequestId: st.paymentRequestId ?? null,
      })),
    };
  }

  async registerSettlement(adminId: string, driverId: string, amountCents: number, notes: string | null, method: string) {
    const [admin, driver] = await Promise.all([
      this.prisma.user.findUnique({ where: { id: adminId } }),
      this.prisma.user.findUnique({ where: { id: driverId, role: UserRole.DRIVER } }),
    ]);
    if (!admin) throw new UnauthorizedException('Sessão inválida — faça login novamente.');
    if (!driver) throw new NotFoundException('Motorista não encontrado');
    if (amountCents <= 0) throw new BadRequestException('Valor deve ser positivo');

    return this.prisma.driverSettlement.create({
      data: { driverId, amountCents, notes, method, settledBy: adminId },
      include: { driver: { select: { name: true } }, settledByUser: { select: { name: true } } },
    });
  }

  async deleteSettlement(settlementId: string) {
    const settlement = await this.prisma.driverSettlement.findUnique({ where: { id: settlementId } });
    if (!settlement) throw new NotFoundException('Liquidação não encontrada');
    return this.prisma.driverSettlement.delete({ where: { id: settlementId } });
  }

  async driverBalance(driverId: string) {
    const [feesAgg, settledAgg, recentRides, recentSettlements] = await Promise.all([
      this.prisma.ride.aggregate({
        where: { driverId, status: RideStatus.FINISHED },
        _sum: { platformFeeCents: true },
      }),
      this.prisma.driverSettlement.aggregate({
        where: { driverId },
        _sum: { amountCents: true },
      }),
      this.prisma.ride.findMany({
        where: { driverId, status: RideStatus.FINISHED, platformFeeCents: { not: null } },
        orderBy: { createdAt: 'desc' },
        take: 10,
        select: { id: true, estimatedFareCents: true, platformFeeCents: true, driverReceivableCents: true, createdAt: true },
      }),
      this.prisma.driverSettlement.findMany({
        where: { driverId },
        orderBy: { settledAt: 'desc' },
        take: 10,
      }),
    ]);

    const totalFeeCents = feesAgg._sum.platformFeeCents ?? 0;
    const totalSettledCents = settledAgg._sum.amountCents ?? 0;
    const balanceCents = Math.max(0, totalFeeCents - totalSettledCents);

    return { totalFeeCents, totalSettledCents, balanceCents, recentRides, recentSettlements };
  }

  async listPaymentRequests(status?: PaymentRequestStatus) {
    return this.prisma.driverPaymentRequest.findMany({
      where: status ? { status } : {},
      orderBy: { requestedAt: 'desc' },
      include: {
        driver: { select: { id: true, name: true, phone: true } },
        reviewer: { select: { name: true } },
        settlement: { select: { id: true, amountCents: true } },
      },
    });
  }

  async confirmPaymentRequest(adminId: string, requestId: string) {
    const [admin, req] = await Promise.all([
      this.prisma.user.findUnique({ where: { id: adminId } }),
      this.prisma.driverPaymentRequest.findUnique({ where: { id: requestId } }),
    ]);
    if (!admin) throw new UnauthorizedException('Sessão inválida — faça login novamente.');
    if (!req) throw new NotFoundException('Solicitação não encontrada');
    if (req.status !== PaymentRequestStatus.PENDING)
      throw new BadRequestException('Solicitação já processada');

    const [settlement] = await this.prisma.$transaction([
      this.prisma.driverSettlement.create({
        data: {
          driverId: req.driverId,
          amountCents: req.amountCents,
          notes: req.notes ?? 'Confirmado via solicitação do motorista',
          method: 'PIX',
          settledBy: adminId,
          paymentRequestId: requestId,
        },
      }),
      this.prisma.driverPaymentRequest.update({
        where: { id: requestId },
        data: { status: PaymentRequestStatus.CONFIRMED, reviewedAt: new Date(), reviewedBy: adminId },
      }),
    ]);
    this.notification.sendToUser(
      req.driverId,
      'Pagamento confirmado!',
      `Seu pagamento de R$ ${(req.amountCents / 100).toFixed(2)} foi confirmado pelo administrador.`,
      { event: 'PAYMENT_CONFIRMED', requestId },
    );
    return settlement;
  }

  async rejectPaymentRequest(adminId: string, requestId: string, reason: string) {
    const req = await this.prisma.driverPaymentRequest.findUnique({ where: { id: requestId } });
    if (!req) throw new NotFoundException('Solicitação não encontrada');
    if (req.status !== PaymentRequestStatus.PENDING)
      throw new BadRequestException('Solicitação já processada');

    const rejected = await this.prisma.driverPaymentRequest.update({
      where: { id: requestId },
      data: {
        status: PaymentRequestStatus.REJECTED,
        reviewedAt: new Date(),
        reviewedBy: adminId,
        rejectionReason: reason ?? 'Rejeitado pelo administrador',
      },
    });
    this.notification.sendToUser(
      rejected.driverId,
      'Pagamento recusado',
      `Seu comprovante foi recusado. Motivo: ${reason ?? 'Sem motivo informado'}. Envie novamente.`,
      { event: 'PAYMENT_REJECTED', requestId },
    );
    return rejected;
  }

  async getSystemConfig() {
    const configs = await this.prisma.systemConfig.findMany();
    return Object.fromEntries(configs.map((c) => [c.key, c.value]));
  }

  async updateSystemConfig(key: string, value: string) {
    return this.prisma.systemConfig.upsert({
      where: { key },
      create: { key, value },
      update: { value },
    });
  }

  // ── Promo Codes ──────────────────────────────────────────────────────────────

  async listPromoCodes() {
    return this.prisma.promoCode.findMany({ orderBy: { id: 'desc' } });
  }

  async createPromoCode(dto: {
    code: string;
    discountPercent?: number;
    discountCents?: number;
    maxUses?: number;
    expiresAt?: string;
  }) {
    const code = dto.code.trim().toUpperCase();
    if (!code) throw new BadRequestException('Código obrigatório');
    if (!dto.discountPercent && !dto.discountCents)
      throw new BadRequestException('Informe desconto em % ou em centavos');
    return this.prisma.promoCode.create({
      data: {
        code,
        discountPercent: dto.discountPercent ?? null,
        discountCents: dto.discountCents ?? null,
        maxUses: dto.maxUses ?? 1,
        expiresAt: dto.expiresAt ? new Date(dto.expiresAt) : null,
      },
    });
  }

  async togglePromoCode(id: string) {
    const promo = await this.prisma.promoCode.findUnique({ where: { id } });
    if (!promo) throw new NotFoundException('Cupom não encontrado');
    return this.prisma.promoCode.update({
      where: { id },
      data: { active: !promo.active },
    });
  }

  async deletePromoCode(id: string) {
    await this.prisma.promoCode.delete({ where: { id } });
    return { ok: true };
  }

  private pricingData(
    dto: CreatePricingConfigDto,
    isActive: boolean,
    regionId: string | null,
  ): Prisma.PricingConfigUncheckedCreateInput {
    return {
      isActive,
      name: dto.name?.trim() || null,
      regionId,
      baseFareCents: dto.baseFareCents ?? 500,
      perKmCents: dto.perKmCents ?? 200,
      perMinuteCents: dto.perMinuteCents ?? 50,
      minimumFareCents: dto.minimumFareCents ?? 800,
      bookingFeeCents: dto.bookingFeeCents ?? 0,
      surgeMultiplier: dto.surgeMultiplier ?? 1.0,
      platformFeePercent: dto.platformFeePercent ?? 20.0,
      currency: dto.currency?.trim() || 'BRL',
    };
  }
}
