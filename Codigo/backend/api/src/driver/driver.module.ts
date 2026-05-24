import { Module } from '@nestjs/common';
import { DriverController } from './driver.controller';
import { DriverService } from './driver.service';
import { PrismaService } from '../prisma/prisma.service';
import { RealtimeModule } from '../realtime/realtime.module';
import { NotificationModule } from '../notification/notification.module';

@Module({
  imports: [RealtimeModule, NotificationModule],
  controllers: [DriverController],
  providers: [DriverService, PrismaService],
})
export class DriverModule {}
