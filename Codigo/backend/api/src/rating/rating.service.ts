import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class RatingService {
  constructor(private prisma: PrismaService) {}

  async createRating(data: {
    rideId: string;
    raterUserId: string;
    score: number;
    comment?: string;
  }) {
    if (data.score < 1 || data.score > 5) {
      throw new BadRequestException('Avaliação deve ser entre 1 e 5');
    }

    const ride = await this.prisma.ride.findUnique({
      where: { id: data.rideId },
    });

    if (!ride) throw new NotFoundException('Corrida não encontrada');
    if (ride.status !== 'FINISHED') {
      throw new BadRequestException('Só é possível avaliar corridas finalizadas');
    }

    const existing = await this.prisma.review.findUnique({
      where: { rideId: data.rideId },
    });
    if (existing) throw new BadRequestException('Você já avaliou esta corrida');

    let ratedUserId: string;
    if (data.raterUserId === ride.passengerId) {
      if (!ride.driverId) throw new BadRequestException('Corrida sem motorista');
      ratedUserId = ride.driverId;
    } else if (data.raterUserId === ride.driverId) {
      ratedUserId = ride.passengerId;
    } else {
      throw new ForbiddenException('Você não participou desta corrida');
    }

    const review = await this.prisma.review.create({
      data: {
        rideId: data.rideId,
        passengerId: data.raterUserId === ride.passengerId ? ride.passengerId : ratedUserId,
        driverId: data.raterUserId === ride.passengerId ? ratedUserId : ride.driverId!,
        rating: data.score,
        comment: data.comment ?? null,
      },
    });

    return {
      ...review,
      score: review.rating,
      raterUserId: data.raterUserId,
      ratedUserId,
    };
  }

  async getMyRatings(userId: string) {
    const ratings = await this.prisma.review.findMany({
      where: { driverId: userId },
      orderBy: { createdAt: 'desc' },
      take: 50,
    });

    const avg =
      ratings.length > 0
        ? ratings.reduce((sum, r) => sum + r.rating, 0) / ratings.length
        : null;

    return {
      ratings: ratings.map((rating) => ({
        ...rating,
        score: rating.rating,
        ratedUserId: rating.driverId,
        raterUserId: rating.passengerId,
      })),
      average: avg ? Math.round(avg * 10) / 10 : null,
      total: ratings.length,
    };
  }
}
