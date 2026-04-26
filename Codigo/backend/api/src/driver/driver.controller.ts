import {
  Body,
  Controller,
  Get,
  Post,
  Patch,
  Param,
  Req,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt.guard';
import { DriverService } from './driver.service';
import { UpdateDriverStatusDto } from './dto/update-driver-status.dto';

@UseGuards(JwtAuthGuard)
@Controller('driver')
export class DriverController {
  constructor(private readonly driverService: DriverService) {}

  @Get('me')
  getDriverMe(@Req() req: any) {
    return this.driverService.getDriverMe(req.user.userId);
  }

  @Post('status')
  updateStatus(@Req() req: any, @Body() dto: UpdateDriverStatusDto) {
    return this.driverService.updateStatus(req.user.userId, dto);
  }

  @Get('rides/pending')
  getPendingRides(@Req() req: any) {
    return this.driverService.getPendingRides(req.user.userId);
  }

  @Post('rides/:rideId/accept')
  acceptRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.acceptRide(req.user.userId, rideId);
  }

  @Post('rides/:rideId/reject')
  rejectRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.rejectRide(req.user.userId, rideId);
  }

  @Get('rides/current')
  getCurrentRide(@Req() req: any) {
    console.log('REQ.USER current ride =>', req.user);
    return this.driverService.getCurrentRide(req.user.userId);
  }

  @Patch('rides/:rideId/start')
  startRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.startRide(req.user.userId, rideId);
  }

  @Patch('rides/:rideId/finish')
  finishRide(@Req() req: any, @Param('rideId') rideId: string) {
    return this.driverService.finishRide(req.user.userId, rideId);
  }
}