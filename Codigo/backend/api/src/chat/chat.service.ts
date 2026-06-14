import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { RideStatus } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class ChatService {
  constructor(private readonly prisma: PrismaService) {}

  private async assertParticipant(userId: string, rideId: string) {
    const ride = await this.prisma.ride.findUnique({
      where: { id: rideId },
      select: { passengerId: true, driverId: true, status: true },
    });

    if (!ride) throw new NotFoundException('Corrida não encontrada');

    const isParticipant =
      ride.passengerId === userId || ride.driverId === userId;

    if (!isParticipant) {
      throw new ForbiddenException(
        'Você não tem permissão para acessar o chat desta corrida',
      );
    }

    // [KL-04 fix] Chat encerrado após finalização ou cancelamento
    const activeChatStatuses: RideStatus[] = [
      RideStatus.ACCEPTED,
      RideStatus.DRIVER_ARRIVING,
      RideStatus.DRIVER_ARRIVED,
      RideStatus.IN_PROGRESS,
    ];
    if (!activeChatStatuses.includes(ride.status)) {
      throw new ForbiddenException('Chat encerrado para esta corrida');
    }

    return ride;
  }

  async sendMessage(
    userId: string,
    rideId: string,
    content: string,
    senderRole: 'PASSENGER' | 'DRIVER',
  ) {
    if (!content || content.trim().length === 0) {
      throw new BadRequestException('O conteúdo da mensagem não pode estar vazio');
    }

    await this.assertParticipant(userId, rideId);

    return this.prisma.chatMessage.create({
      data: {
        rideId,
        senderId: userId,
        senderRole,
        content: content.trim(),
      },
    });
  }

  async getMessages(userId: string, rideId: string) {
    await this.assertParticipant(userId, rideId);

    const messages = await this.prisma.chatMessage.findMany({
      where: { rideId },
      orderBy: { sentAt: 'asc' },
      take: 100,
    });

    // Mark unread messages (sent by others) as read
    const unreadIds = messages
      .filter((m) => m.senderId !== userId && m.readAt === null)
      .map((m) => m.id);

    if (unreadIds.length > 0) {
      await this.prisma.chatMessage.updateMany({
        where: { id: { in: unreadIds } },
        data: { readAt: new Date() },
      });
    }

    return messages;
  }
}
