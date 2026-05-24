import { Module } from '@nestjs/common';
import { RideModule } from '../ride/ride.module';
import { PaymentController } from './payment.controller';

@Module({
  imports: [RideModule],
  controllers: [PaymentController],
})
export class PaymentModule {}
