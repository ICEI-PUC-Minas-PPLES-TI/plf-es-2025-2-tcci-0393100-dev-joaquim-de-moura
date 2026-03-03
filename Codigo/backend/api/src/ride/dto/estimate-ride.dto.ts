import { Type } from 'class-transformer';
import { IsNumber } from 'class-validator';

export class EstimateRideDto {
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
}