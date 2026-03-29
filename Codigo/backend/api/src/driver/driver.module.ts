import { Module } from '@nestjs/common';
import { DriverController } from './driver.controller';
import { DriverService } from './driver.service';
import { PrismaService } from '../prisma/prisma.service';

@Module({
  controllers: [DriverController],
  providers: [DriverService, PrismaService],
})
export class DriverModule {}