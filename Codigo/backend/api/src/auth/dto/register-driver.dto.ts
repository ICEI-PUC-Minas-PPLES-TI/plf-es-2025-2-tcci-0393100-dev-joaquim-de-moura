import { IsBoolean, IsOptional, IsString, MinLength } from 'class-validator';

export class RegisterDriverDto {
  @IsString()
  phone: string;

  @IsString()
  name: string;

  @IsString()
  @MinLength(4)
  password: string;

  @IsOptional()
  @IsString()
  cnhImageUrl?: string;

  @IsOptional()
  @IsString()
  cnhNumber?: string;

  @IsOptional()
  @IsString()
  cnhCategory?: string;

  @IsBoolean()
  hasEar: boolean;
}