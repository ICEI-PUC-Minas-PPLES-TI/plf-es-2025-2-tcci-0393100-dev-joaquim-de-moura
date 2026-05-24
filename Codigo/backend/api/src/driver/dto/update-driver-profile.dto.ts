import { IsBoolean, IsInt, IsOptional, IsString, MaxLength } from 'class-validator';

export class UpdateDriverProfileDto {
  @IsOptional()
  @IsString()
  @MaxLength(100)
  name?: string;

  @IsOptional()
  @IsString()
  @MaxLength(20)
  cnhNumber?: string;

  @IsOptional()
  @IsString()
  @MaxLength(5)
  cnhCategory?: string;

  @IsOptional()
  @IsBoolean()
  hasEar?: boolean;

  @IsOptional()
  @IsString()
  pixQrPayload?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  pixKey?: string;

  @IsOptional()
  @IsString()
  @MaxLength(160)
  pixQrCodeUrl?: string;

  @IsOptional()
  @IsString()
  @MaxLength(80)
  vehicleModel?: string;

  @IsOptional()
  @IsString()
  @MaxLength(12)
  vehiclePlate?: string;

  @IsOptional()
  @IsString()
  @MaxLength(40)
  vehicleColor?: string;

  @IsOptional()
  @IsString()
  @MaxLength(14)
  cpf?: string;

  @IsOptional()
  @IsInt()
  vehicleYear?: number;

  @IsOptional()
  @IsString()
  @MaxLength(10)
  cnhExpiresAt?: string;

  @IsOptional()
  @IsInt()
  vehicleCapacity?: number;
}
