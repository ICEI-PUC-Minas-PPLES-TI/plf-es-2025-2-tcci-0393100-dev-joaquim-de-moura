import { IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';
import { SupportTicketStatus } from '@prisma/client';

export class UpdateSupportTicketDto {
  @IsOptional()
  @IsEnum(SupportTicketStatus)
  status?: SupportTicketStatus;

  @IsOptional()
  @IsString()
  @MaxLength(1000)
  resolution?: string;
}
