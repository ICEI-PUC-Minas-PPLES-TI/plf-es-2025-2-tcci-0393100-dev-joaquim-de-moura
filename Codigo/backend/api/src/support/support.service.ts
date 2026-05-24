import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, SupportTicketStatus, UserRole } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { CreateSupportTicketDto } from './dto/create-support-ticket.dto';
import { UpdateSupportTicketDto } from './dto/update-support-ticket.dto';

type RequestUser = {
  userId: string;
  role: UserRole;
};

type TicketWithRelations = Prisma.SupportTicketGetPayload<{
  include: {
    creator: { select: { id: true; name: true; phone: true; role: true } };
    ride: {
      select: {
        id: true;
        status: true;
        paymentStatus: true;
        originAddress: true;
        destinationAddress: true;
        passengerId: true;
        driverId: true;
      };
    };
  };
}>;

@Injectable()
export class SupportService {
  constructor(private prisma: PrismaService) {}

  private include() {
    return {
      creator: { select: { id: true, name: true, phone: true, role: true } },
      ride: {
        select: {
          id: true,
          status: true,
          paymentStatus: true,
          originAddress: true,
          destinationAddress: true,
          passengerId: true,
          driverId: true,
        },
      },
    } satisfies Prisma.SupportTicketInclude;
  }

  private toResponse(ticket: TicketWithRelations) {
    return {
      id: ticket.id,
      creatorId: ticket.creatorId,
      rideId: ticket.rideId,
      type: ticket.type,
      status: ticket.status,
      subject: ticket.subject,
      description: ticket.description,
      resolution: ticket.resolution,
      createdAt: ticket.createdAt,
      updatedAt: ticket.updatedAt,
      closedAt: ticket.closedAt,
      creator: ticket.creator,
      ride: ticket.ride,
    };
  }

  async createTicket(user: RequestUser, dto: CreateSupportTicketDto) {
    const rideId = dto.rideId?.trim() || undefined;

    if (rideId) {
      const ride = await this.prisma.ride.findUnique({ where: { id: rideId } });

      const canReference =
        !!ride &&
        (user.role === UserRole.ADMIN ||
          ride.passengerId === user.userId ||
          ride.driverId === user.userId);

      if (!canReference) {
        throw new NotFoundException('Corrida não encontrada para este usuário');
      }
    }

    const ticket = await this.prisma.supportTicket.create({
      data: {
        creatorId: user.userId,
        rideId,
        type: dto.type,
        subject: dto.subject.trim(),
        description: dto.description.trim(),
      },
      include: this.include(),
    });

    return this.toResponse(ticket);
  }

  async listMine(user: RequestUser) {
    const tickets = await this.prisma.supportTicket.findMany({
      where: { creatorId: user.userId },
      include: this.include(),
      orderBy: { createdAt: 'desc' },
      take: 100,
    });

    return tickets.map((ticket) => this.toResponse(ticket));
  }

  async listAll(status?: SupportTicketStatus) {
    const tickets = await this.prisma.supportTicket.findMany({
      where: status ? { status } : {},
      include: this.include(),
      orderBy: { createdAt: 'desc' },
      take: 200,
    });

    return tickets.map((ticket) => this.toResponse(ticket));
  }

  async updateTicket(ticketId: string, dto: UpdateSupportTicketDto) {
    const existing = await this.prisma.supportTicket.findUnique({
      where: { id: ticketId },
    });

    if (!existing) {
      throw new NotFoundException('Chamado não encontrado');
    }

    if (!dto.status && dto.resolution === undefined) {
      throw new BadRequestException('Informe status ou resolução para atualizar o chamado');
    }

    const nextStatus = dto.status ?? existing.status;
    const shouldClose =
      nextStatus === SupportTicketStatus.RESOLVED ||
      nextStatus === SupportTicketStatus.CLOSED;

    const ticket = await this.prisma.supportTicket.update({
      where: { id: ticketId },
      data: {
        status: nextStatus,
        resolution: dto.resolution?.trim() || null,
        closedAt: shouldClose ? (existing.closedAt ?? new Date()) : null,
      },
      include: this.include(),
    });

    return this.toResponse(ticket);
  }
}
