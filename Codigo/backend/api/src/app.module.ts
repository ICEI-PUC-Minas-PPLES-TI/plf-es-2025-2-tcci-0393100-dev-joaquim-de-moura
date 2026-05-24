import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ScheduleModule } from '@nestjs/schedule';
import { MapsModule } from './maps/maps.module';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { RideModule } from './ride/ride.module';
import { AuthModule } from './auth/auth.module';
import { PrismaModule } from './prisma/prisma.module';
import { DriverModule } from './driver/driver.module';
import { AdminModule } from './admin/admin.module';
import { PaymentModule } from './payment/payment.module';
import { ReviewModule } from './review/review.module';
import { RatingModule } from './rating/rating.module';
import { SupportModule } from './support/support.module';
import { RealtimeModule } from './realtime/realtime.module';
import { ChatModule } from './chat/chat.module';
import { validateEnv } from './config/env.validation';

@Module({
  imports: [
    ScheduleModule.forRoot(),
    ConfigModule.forRoot({ isGlobal: true, validate: validateEnv }),
    MapsModule,
    RideModule,
    PrismaModule,
    AuthModule,
    DriverModule,
    AdminModule,
    PaymentModule,
    ReviewModule,
    RatingModule,
    SupportModule,
    RealtimeModule,
    ChatModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
