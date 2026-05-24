import { IsOptional, IsString, MaxLength } from 'class-validator';

export class ConfirmPaymentDto {
  @IsOptional()
  @IsString()
  rideId?: string;

  @IsOptional()
  @IsString()
  @MaxLength(35)
  txId?: string;

  @IsOptional()
  @IsString()
  @MaxLength(240)
  receiptNote?: string;
}

