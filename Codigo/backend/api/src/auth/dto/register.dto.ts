import {
  IsBoolean,
  IsDateString,
  IsEmail,
  IsEnum,
  IsOptional,
  IsString,
  Matches,
  MinLength,
} from 'class-validator';
import { UserRole } from '@prisma/client';

export class RegisterDto {
  @IsString()
  phone: string;

  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsEmail()
  email?: string;

  @IsString()
  @MinLength(6)
  password: string;

  @IsOptional()
  @IsEnum(UserRole)
  role?: UserRole; // PASSENGER/DRIVER/ADMIN (se quiser permitir)

  @IsOptional()
  @IsString()
  @Matches(/^\d{11}$/, { message: 'CPF deve conter 11 dígitos' })
  cpf?: string;

  @IsOptional()
  @IsDateString({}, { message: 'Data de nascimento inválida' })
  birthDate?: string;

  @IsOptional()
  @IsString()
  emergencyContactName?: string;

  @IsOptional()
  @IsString()
  @Matches(/^\d{10,15}$/, { message: 'Telefone de emergência inválido' })
  emergencyContactPhone?: string;

  @IsOptional()
  @IsString()
  accessibilityNotes?: string;

  @IsOptional()
  @IsBoolean()
  acceptedTerms?: boolean;

  @IsOptional()
  @IsBoolean()
  acceptedPrivacy?: boolean;
}
