import { IsBoolean } from 'class-validator';

export class UpdateDriverStatusDto {

  @IsBoolean()
  online: boolean;

}