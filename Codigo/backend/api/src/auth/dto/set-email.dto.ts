import { IsEmail, MaxLength } from 'class-validator';

export class SetEmailDto {
  @IsEmail()
  @MaxLength(120)
  email!: string;
}
