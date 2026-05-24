import { Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
import { SupportTicketStatus, UserRole } from '@prisma/client';
import { JwtAuthGuard } from '../auth/jwt.guard';
import { Roles } from '../auth/roles.decorator';
import { RolesGuard } from '../auth/roles.guard';
import { CreateSupportTicketDto } from './dto/create-support-ticket.dto';
import { UpdateSupportTicketDto } from './dto/update-support-ticket.dto';
import { SupportService } from './support.service';

@UseGuards(JwtAuthGuard)
@Controller('support')
export class SupportController {
  constructor(private readonly supportService: SupportService) {}

  @Post('tickets')
  createTicket(@Req() req: any, @Body() dto: CreateSupportTicketDto) {
    return this.supportService.createTicket(req.user, dto);
  }

  @Get('tickets/my')
  listMine(@Req() req: any) {
    return this.supportService.listMine(req.user);
  }

  @UseGuards(RolesGuard)
  @Roles(UserRole.ADMIN)
  @Get('tickets')
  listAll(@Query('status') status?: SupportTicketStatus) {
    return this.supportService.listAll(status);
  }

  @UseGuards(RolesGuard)
  @Roles(UserRole.ADMIN)
  @Patch('tickets/:ticketId')
  updateTicket(
    @Param('ticketId') ticketId: string,
    @Body() dto: UpdateSupportTicketDto,
  ) {
    return this.supportService.updateTicket(ticketId, dto);
  }
}
