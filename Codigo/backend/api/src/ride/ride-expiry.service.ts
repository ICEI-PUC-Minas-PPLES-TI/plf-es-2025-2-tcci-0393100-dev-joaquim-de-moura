import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { PrismaService } from '../prisma/prisma.service';
import { RealtimeService } from '../realtime/realtime.service';
import { RideStatus } from '@prisma/client';

@Injectable()
export class RideExpiryService {
  private readonly logger = new Logger(RideExpiryService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly realtime: RealtimeService,
  ) {}

  /** Cancela corridas PENDING_DRIVER com mais de 10 minutos sem motorista */
  @Cron(CronExpression.EVERY_MINUTE)
  async cancelStalePendingRides() {
    const cutoff = new Date(Date.now() - 10 * 60 * 1000);

    const stale = await this.prisma.ride.findMany({
      where: { status: RideStatus.PENDING_DRIVER, createdAt: { lt: cutoff } },
      select: { id: true, passengerId: true },
    });

    if (!stale.length) return;

    await this.prisma.ride.updateMany({
      where: { id: { in: stale.map((r) => r.id) } },
      data: { status: RideStatus.CANCELED },
    });

    for (const ride of stale) {
      this.realtime.emitToUser(ride.passengerId, 'ride_status_update', {
        rideId: ride.id,
        status: 'CANCELED',
        reason: 'Nenhum motorista disponível no momento. Tente novamente.',
      });
      this.logger.log(`Auto-cancelada ${ride.id}: PENDING_DRIVER > 10 min`);
    }
  }

  /** Cancela corridas ACCEPTED com mais de 30 minutos paradas (motorista não apareceu) */
  @Cron(CronExpression.EVERY_5_MINUTES)
  async cancelStaleAcceptedRides() {
    const cutoff = new Date(Date.now() - 30 * 60 * 1000);

    const stale = await this.prisma.ride.findMany({
      where: { status: RideStatus.ACCEPTED, updatedAt: { lt: cutoff } },
      select: { id: true, passengerId: true },
    });

    if (!stale.length) return;

    await this.prisma.ride.updateMany({
      where: { id: { in: stale.map((r) => r.id) } },
      data: { status: RideStatus.CANCELED },
    });

    for (const ride of stale) {
      this.realtime.emitToUser(ride.passengerId, 'ride_status_update', {
        rideId: ride.id,
        status: 'CANCELED',
        reason: 'Motorista não chegou no tempo esperado.',
      });
      this.logger.log(`Auto-cancelada ${ride.id}: ACCEPTED > 30 min`);
    }
  }
}
