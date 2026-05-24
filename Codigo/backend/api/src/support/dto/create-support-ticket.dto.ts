import { IsEnum, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';
import { SupportTicketType } from '@prisma/client';

export class CreateSupportTicketDto {
  @IsOptional()
  @IsString()
  rideId?: string;

  @IsOptional()
  @IsEnum(SupportTicketType)
  type?: SupportTicketType;

  @IsString()
  @MinLength(4)
  @MaxLength(120)
  subject: string;

  @IsString()
  @MinLength(10)
  @MaxLength(1000)
  description: string;
}
