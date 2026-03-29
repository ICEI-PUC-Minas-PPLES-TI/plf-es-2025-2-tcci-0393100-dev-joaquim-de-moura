import {
  Body,
  Controller,
  Get,
  Param,
  Post,
  Req,
  UseGuards,
} from '@nestjs/common';
import { DriverService } from './driver.service';
import { UpdateDriverStatusDto } from './dto/update-driver-status.dto';
import { JwtAuthGuard } from '../auth/jwt.guard';

@Controller('driver')
@UseGuards(JwtAuthGuard)
export class DriverController {
  constructor(private readonly driverService: DriverService) {}

  @Get('me')
  getDriverMe(@Req() req) {
    return this.driverService.getDriverMe(req.user.userId);
  }

  @Post('status')
  updateStatus(@Req() req, @Body() dto: UpdateDriverStatusDto) {
    return this.driverService.updateStatus(req.user.userId, dto);
  }

  @Get('rides/pending')
  getPendingRides(@Req() req) {
    return this.driverService.getPendingRides(req.user.userId);
  }

  @Post('rides/:rideId/accept')
  acceptRide(@Req() req, @Param('rideId') rideId: string) {
    return this.driverService.acceptRide(req.user.userId, rideId);
  }

  @Post('rides/:rideId/reject')
  rejectRide(@Req() req, @Param('rideId') rideId: string) {
    return this.driverService.rejectRide(req.user.userId, rideId);
  }
}