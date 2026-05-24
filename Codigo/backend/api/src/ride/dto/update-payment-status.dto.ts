import { IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';
import { PaymentStatus } from '@prisma/client';

export class UpdatePaymentStatusDto {
  @IsEnum(PaymentStatus)
  paymentStatus: PaymentStatus;

  @IsOptional()
  @IsString()
  @MaxLength(35)
  txId?: string;

  @IsOptional()
  @IsString()
  @MaxLength(240)
  receiptNote?: string;
}
