import { Body, Controller, Get, Param, Post, Req, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt.guard';
import { CreateReviewDto } from './dto/create-review.dto';
import { ReviewService } from './review.service';

@UseGuards(JwtAuthGuard)
@Controller('reviews')
export class ReviewController {
  constructor(private readonly reviewService: ReviewService) {}

  @Post('rides/:rideId')
  createForRide(
    @Req() req: any,
    @Param('rideId') rideId: string,
    @Body() dto: CreateReviewDto,
  ) {
    return this.reviewService.createForRide(req.user.userId, rideId, dto);
  }

  @Get('drivers/:driverId')
  listForDriver(@Param('driverId') driverId: string) {
    return this.reviewService.listForDriver(driverId);
  }

  @Get('me')
  listMine(@Req() req: any) {
    return this.reviewService.listMine(req.user);
  }
}
