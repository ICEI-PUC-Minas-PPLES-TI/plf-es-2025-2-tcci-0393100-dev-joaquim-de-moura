import {
  BadRequestException,
  Body,
  Controller,
  Param,
  Patch,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt.guard';
import { UpdatePaymentStatusDto } from '../ride/dto/update-payment-status.dto';
import { RideService } from '../ride/ride.service';
import { ConfirmPaymentDto } from './dto/confirm-payment.dto';

type AuthRequest = {
  user: { userId: string; role: string };
};

@UseGuards(JwtAuthGuard)
@Controller('payments')
export class PaymentController {
  constructor(private readonly rideService: RideService) {}

  @Patch('rides/:rideId/status')
  updateRidePaymentStatus(
    @Req() req: AuthRequest,
    @Param('rideId') rideId: string,
    @Body() dto: UpdatePaymentStatusDto,
  ) {
    return this.rideService.updatePaymentStatus(
      rideId,
      dto.paymentStatus,
      req.user,
      dto,
    );
  }

  @Post('confirm')
  confirmPaymentByBody(
    @Req() req: AuthRequest,
    @Body() dto: ConfirmPaymentDto,
  ) {
    if (!dto.rideId) {
      throw new BadRequestException('rideId é obrigatório para confirmar pagamento');
    }

    return this.rideService.confirmPayment(dto.rideId, req.user, dto);
  }

  @Post('rides/:rideId/confirm')
  confirmRidePayment(
    @Req() req: AuthRequest,
    @Param('rideId') rideId: string,
    @Body() dto: ConfirmPaymentDto,
  ) {
    return this.rideService.confirmPayment(rideId, req.user, dto);
  }

  @Post('rides/:rideId/not-received')
  markRidePaymentNotReceived(
    @Req() req: AuthRequest,
    @Param('rideId') rideId: string,
    @Body() dto: ConfirmPaymentDto,
  ) {
    return this.rideService.markPaymentNotReceived(rideId, req.user, dto);
  }
}
