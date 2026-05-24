import { IsString, Matches } from 'class-validator';

export class ConfirmVerificationDto {
  @IsString()
  @Matches(/^\d{6}$/)
  code!: string;
}
