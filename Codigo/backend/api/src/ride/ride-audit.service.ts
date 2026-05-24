import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { Prisma, UserRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

type RequestUser = { userId: string; role: UserRole };

@Injectable()
export class RideAuditService {
  constructor(private prisma: PrismaService) {}

  async append(
    rideId: string,
    actorUserId: string | null,
    action: string,
    metadata?: Record<string, unknown>,
  ) {
    await this.prisma.rideAuditLog.create({
      data: {
        rideId,
        actorUserId,
        action,
        metadata: (metadata ?? undefined) as Prisma.InputJsonValue | undefined,
      },
    });
  }

  async assertRideParticipant(rideId: string, user: RequestUser) {
    const ride = await this.prisma.ride.findUnique({
      where: { id: rideId },
      select: { id: true, passengerId: true, driverId: true },
    });

    if (!ride) throw new NotFoundException('Corrida não encontrada');

    const ok =
      user.role === UserRole.ADMIN ||
      ride.passengerId === user.userId ||
      ride.driverId === user.userId;

    if (!ok) throw new ForbiddenException('Sem acesso a esta corrida');

    return ride;
  }

  async listForRide(rideId: string, user: RequestUser) {
    await this.assertRideParticipant(rideId, user);

    return this.prisma.rideAuditLog.findMany({
      where: { rideId },
      orderBy: { createdAt: 'asc' },
      select: {
        id: true,
        action: true,
        metadata: true,
        createdAt: true,
        actorUserId: true,
      },
    });
  }
}
