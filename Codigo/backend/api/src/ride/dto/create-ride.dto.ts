import { IsEnum, IsNotEmpty, IsNumber, IsOptional, IsString } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateRideDto {
  @Type(() => Number)
  @IsNumber()
  originLat: number;

  @Type(() => Number)
  @IsNumber()
  originLng: number;

  @Type(() => Number)
  @IsNumber()
  destLat: number;

  @Type(() => Number)
  @IsNumber()
  destLng: number;

  @IsNotEmpty()
  @IsString()
  originAddress: string;

  @IsNotEmpty()
  @IsString()
  destinationAddress: string;

  @IsEnum(['CASH', 'PIX'])
  paymentMethod: string;

  @IsOptional()
  @IsString()
  promoCode?: string;
}