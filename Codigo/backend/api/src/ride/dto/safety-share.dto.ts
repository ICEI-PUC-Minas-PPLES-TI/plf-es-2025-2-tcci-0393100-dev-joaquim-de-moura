import { IsOptional, IsString, MaxLength } from 'class-validator';

export class SafetyShareDto {
  @IsOptional()
  @IsString()
  @MaxLength(40)
  channel?: string;
}
