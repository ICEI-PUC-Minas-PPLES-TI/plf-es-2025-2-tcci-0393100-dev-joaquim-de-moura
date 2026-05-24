import { IsString, Matches } from 'class-validator';

export class RequestPasswordResetDto {
  @IsString()
  @Matches(/^\d{10,15}$/, { message: 'Informe um telefone válido' })
  phone!: string;
}
