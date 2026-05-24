import { IsEnum, IsOptional, IsString } from 'class-validator';
import { PaymentStatus, RideStatus } from '@prisma/client';

export class ListRidesQueryDto {
  @IsOptional()
  @IsEnum(RideStatus)
  status?: RideStatus;

  @IsOptional()
  @IsEnum(PaymentStatus)
  paymentStatus?: PaymentStatus;

  @IsOptional()
  @IsString()
  driverId?: string;

  @IsOptional()
  @IsString()
  passengerId?: string;
}
