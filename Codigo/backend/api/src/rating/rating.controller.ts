import { Body, Controller, Get, Post, Req, UseGuards } from '@nestjs/common';
import { RatingService } from './rating.service';
import { JwtAuthGuard } from '../auth/jwt.guard';

@UseGuards(JwtAuthGuard)
@Controller('ratings')
export class RatingController {
  constructor(private readonly ratingService: RatingService) {}

  @Post()
  createRating(
    @Req() req: any,
    @Body() body: { rideId: string; score: number; comment?: string },
  ) {
    return this.ratingService.createRating({
      rideId: body.rideId,
      raterUserId: req.user.userId,
      score: body.score,
      comment: body.comment,
    });
  }

  @Get('me')
  getMyRatings(@Req() req: any) {
    return this.ratingService.getMyRatings(req.user.userId);
  }
}
