import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { MapsModule } from './maps/maps.module';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { RideModule } from './ride/ride.module';
import { AuthModule } from './auth/auth.module';
import { PrismaModule } from './prisma/prisma.module';
import { DriverModule } from './driver/driver.module';
import { AdminModule } from './admin/admin.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    MapsModule,
    RideModule,
    PrismaModule,
    AuthModule,
    DriverModule,
    AdminModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}