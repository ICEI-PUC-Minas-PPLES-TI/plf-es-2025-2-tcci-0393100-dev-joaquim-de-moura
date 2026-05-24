import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import { RideStatus } from '@prisma/client';
import { RideService } from './ride.service';
import { EstimateRideDto } from './dto/estimate-ride.dto';
import { JwtAuthGuard } from '../auth/jwt.guard';

@Controller('rides')
export class RideController {
  constructor(private readonly service: RideService) {}

  // Passageiro cria corrida — requer autenticação
  @UseGuards(JwtAuthGuard)
  @Post()
  create(@Req() req: any, @Body() body: any) {
    return this.service.create({
      passengerId: req.user.userId,
      originLat: body.originLat,
      originLng: body.originLng,
      destLat: body.destLat,
      destLng: body.destLng,
      originAddress: body.originAddress,
      destinationAddress: body.destinationAddress,
      paymentMethod: body.paymentMethod,
      promoCode: body.promoCode,
    });
  }

  // Estimativa de preço — pública
  @Post('estimate')
  estimate(@Body() dto: EstimateRideDto) {
    return this.service.estimate(dto);
  }

  // Valida cupom de desconto — requer autenticação
  @UseGuards(JwtAuthGuard)
  @Post('validate-coupon')
  validateCoupon(@Body() body: { code: string }) {
    return this.service.validateCoupon(body.code);
  }

  // Histórico de corridas do passageiro — requer autenticação
  @UseGuards(JwtAuthGuard)
  @Get('my-rides')
  myRides(@Req() req: any) {
    return this.service.myRides(req.user.userId);
  }

  // Corridas abertas aguardando motorista — pública (motoristas não autenticados podem ver)
  @Get('open')
  findOpen() {
    return this.service.findOpen();
  }

  // Detalhes de uma corrida — passageiro acompanha o status
  @UseGuards(JwtAuthGuard)
  @Get(':id')
  findById(@Req() req: any, @Param('id') id: string) {
    return this.service.findById(id, req.user.userId, req.user.role);
  }

  // Passageiro cancela corrida — requer autenticação
  @UseGuards(JwtAuthGuard)
  @Patch(':id/status')
  updateStatus(
    @Req() req: any,
    @Param('id') id: string,
    @Body() body: { status: RideStatus },
  ) {
    return this.service.updateStatus(id, body.status, req.user.userId, req.user.role);
  }
}
