import { IsString, Matches, MinLength } from 'class-validator';

export class ConfirmPasswordResetDto {
  @IsString()
  @Matches(/^\d{10,15}$/, { message: 'Informe um telefone válido' })
  phone!: string;

  @IsString()
  @Matches(/^\d{6}$/, { message: 'Informe o código de 6 dígitos' })
  code!: string;

  @IsString()
  @MinLength(8, { message: 'A nova senha deve ter pelo menos 8 caracteres' })
  @Matches(/^(?=.*[A-Za-z])(?=.*\d).+$/, {
    message: 'A nova senha deve conter letras e números',
  })
  newPassword!: string;
}
