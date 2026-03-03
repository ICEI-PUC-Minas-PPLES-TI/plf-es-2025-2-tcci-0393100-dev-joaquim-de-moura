import { Module } from '@nestjs/common';
import { RideController } from './ride.controller';
import { RideService } from './ride.service';
import { PrismaModule } from '../prisma/prisma.module';
import { MapsModule } from '../maps/maps.module';

@Module({
  imports: [PrismaModule, MapsModule],
  controllers: [RideController],
  providers: [RideService],
})
export class RideModule {}