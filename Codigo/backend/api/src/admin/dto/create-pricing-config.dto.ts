import { Type } from 'class-transformer';
import { IsBoolean, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';

export class CreatePricingConfigDto {
  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsString()
  regionId?: string;

  @IsOptional()
  @IsBoolean()
  isActive?: boolean;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  baseFareCents?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  perKmCents?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  perMinuteCents?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  minimumFareCents?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  bookingFeeCents?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0.5)
  @Max(5)
  surgeMultiplier?: number;

  @IsOptional()
  @IsString()
  currency?: string;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(0)
  @Max(100)
  platformFeePercent?: number;
}
