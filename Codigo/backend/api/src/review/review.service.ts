import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, RideStatus, UserRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CreateReviewDto } from './dto/create-review.dto';

type RequestUser = {
  userId: string;
  role: UserRole;
};

type ReviewWithRelations = Prisma.ReviewGetPayload<{
  include: {
    passenger: { select: { id: true; name: true; phone: true } };
    driver: { select: { id: true; name: true; phone: true } };
    ride: {
      select: {
        id: true;
        originAddress: true;
        destinationAddress: true;
        estimatedFareCents: true;
        finishedAt: true;
      };
    };
  };
}>;

@Injectable()
export class ReviewService {
  constructor(private prisma: PrismaService) {}

  private include() {
    return {
      passenger: { select: { id: true, name: true, phone: true } },
      driver: { select: { id: true, name: true, phone: true } },
      ride: {
        select: {
          id: true,
          originAddress: true,
          destinationAddress: true,
          estimatedFareCents: true,
          finishedAt: true,
        },
      },
    } satisfies Prisma.ReviewInclude;
  }

  private toResponse(review: ReviewWithRelations) {
    return {
      id: review.id,
      rideId: review.rideId,
      passengerId: review.passengerId,
      driverId: review.driverId,
      rating: review.rating,
      comment: review.comment,
      createdAt: review.createdAt,
      passenger: review.passenger,
      driver: review.driver,
      ride: review.ride,
    };
  }

  async createForRide(userId: string, rideId: string, dto: CreateReviewDto) {
    const ride = await this.prisma.ride.findUnique({
      where: { id: rideId },
    });

    if (!ride || ride.passengerId !== userId) {
      throw new NotFoundException('Corrida não encontrada');
    }

    if (ride.status !== RideStatus.FINISHED) {
      throw new BadRequestException('A avaliação só pode ser enviada após finalizar a corrida');
    }

    if (!ride.driverId) {
      throw new BadRequestException('Corrida sem motorista vinculado');
    }

    const existing = await this.prisma.review.findUnique({
      where: { rideId },
    });

    if (existing) {
      throw new BadRequestException('Essa corrida já foi avaliada');
    }

    // [KL-05 fix] Encapsula P2002 (unique constraint em rideId) para evitar vazamento de detalhes internos
    let review;
    try {
      review = await this.prisma.review.create({
        data: {
          rideId,
          passengerId: userId,
          driverId: ride.driverId,
          rating: dto.rating,
          comment: dto.comment?.trim() || null,
        },
        include: this.include(),
      });
    } catch (err) {
      if (err instanceof Prisma.PrismaClientKnownRequestError && err.code === 'P2002') {
        throw new BadRequestException('Essa corrida já foi avaliada');
      }
      throw err;
    }

    return this.toResponse(review);
  }

  async listForDriver(driverIdOrProfileId: string) {
    const driverProfile = await this.prisma.driverProfile.findFirst({
      where: {
        OR: [{ id: driverIdOrProfileId }, { userId: driverIdOrProfileId }],
      },
      select: { userId: true },
    });

    const driverId = driverProfile?.userId ?? driverIdOrProfileId;

    const reviews = await this.prisma.review.findMany({
      where: { driverId },
      include: this.include(),
      orderBy: { createdAt: 'desc' },
      take: 100,
    });

    const aggregate = await this.prisma.review.aggregate({
      where: { driverId },
      _avg: { rating: true },
      _count: { rating: true },
    });

    return {
      averageRating: aggregate._avg.rating ?? null,
      totalReviews: aggregate._count.rating,
      reviews: reviews.map((review) => this.toResponse(review)),
    };
  }

  async listMine(user: RequestUser) {
    const where: Prisma.ReviewWhereInput =
      user.role === UserRole.ADMIN
        ? {}
        : user.role === UserRole.DRIVER
          ? { driverId: user.userId }
          : { passengerId: user.userId };

    const reviews = await this.prisma.review.findMany({
      where,
      include: this.include(),
      orderBy: { createdAt: 'desc' },
      take: 100,
    });

    return reviews.map((review) => this.toResponse(review));
  }
}
