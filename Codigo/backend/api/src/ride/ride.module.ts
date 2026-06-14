import { Module } from '@nestjs/common';
import { RideController } from './ride.controller';
import { RideSafetyController } from './ride-safety.controller';
import { RideService } from './ride.service';
import { RideExpiryService } from './ride-expiry.service';
import { RideAuditService } from './ride-audit.service';
import { PrismaModule } from '../prisma/prisma.module';
import { MapsModule } from '../maps/maps.module';
import { RealtimeModule } from '../realtime/realtime.module';
import { NotificationModule } from '../notification/notification.module';

@Module({
  imports: [PrismaModule, MapsModule, RealtimeModule, NotificationModule],
  controllers: [RideController, RideSafetyController],
  providers: [RideService, RideExpiryService, RideAuditService],
  exports: [RideService],
})
export class RideModule {}
