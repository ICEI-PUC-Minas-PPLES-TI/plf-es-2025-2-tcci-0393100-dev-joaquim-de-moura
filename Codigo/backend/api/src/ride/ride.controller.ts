import { Body, Controller, Get, Param, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { RideStatus } from '@prisma/client';
import { RideService } from './ride.service';
import { EstimateRideDto } from './dto/estimate-ride.dto';
import { JwtAuthGuard } from '../auth/jwt.guard';

@Controller('rides')
export class RideController {
  constructor(private readonly service: RideService) {}

  @UseGuards(JwtAuthGuard)
  @Post()
  create(@Req() req: any, @Body() body: any) {
    return this.service.create({
      passengerId: req.user.userId, // vem do token
      originLat: body.originLat,
      originLng: body.originLng,
      destLat: body.destLat,
      destLng: body.destLng,
    });
  }

  @Get()
  findAll() {
    return this.service.findAll();
  }

  @Get('open')
  findOpen() {
    return this.service.findOpen();
  }

  @Post(':id/accept')
  acceptRide(@Param('id') id: string, @Body() body: { driverId: string }) {
    return this.service.acceptRide(id, body.driverId);
  }

  @Post('estimate')
  estimate(@Body() dto: EstimateRideDto) {
    return this.service.estimate(dto);
  }

  @Patch(':id/status')
  updateStatus(@Param('id') id: string, @Body() body: { status: RideStatus }) {
    return this.service.updateStatus(id, body.status);
  }
}