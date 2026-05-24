import { IsNumber, IsOptional, IsString, MaxLength } from 'class-validator';

export class SafetyEmergencyDto {
  @IsOptional()
  @IsString()
  @MaxLength(500)
  note?: string;

  @IsOptional()
  @IsNumber()
  lat?: number;

  @IsOptional()
  @IsNumber()
  lng?: number;
}
