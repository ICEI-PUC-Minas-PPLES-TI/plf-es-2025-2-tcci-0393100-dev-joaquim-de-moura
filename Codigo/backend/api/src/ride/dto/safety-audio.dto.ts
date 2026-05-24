import { IsBoolean, IsIn, IsNumber, IsOptional, Min } from 'class-validator';

export class SafetyAudioDto {
  @IsIn(['start', 'stop'])
  phase!: 'start' | 'stop';

  @IsOptional()
  @IsBoolean()
  consentAccepted?: boolean;

  @IsOptional()
  @IsNumber()
  @Min(0)
  durationMs?: number;
}
