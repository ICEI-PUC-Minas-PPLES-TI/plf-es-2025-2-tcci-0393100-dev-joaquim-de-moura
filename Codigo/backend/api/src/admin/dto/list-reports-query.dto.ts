import { IsDateString, IsOptional } from 'class-validator';

export class ListReportsQueryDto {
  @IsOptional()
  @IsDateString()
  from?: string;

  @IsOptional()
  @IsDateString()
  to?: string;
}
