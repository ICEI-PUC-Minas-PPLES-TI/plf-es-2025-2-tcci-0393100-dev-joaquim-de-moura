import { IsBoolean, IsOptional } from 'class-validator';

export class UpdateDriverStatusDto {
  @IsBoolean()
  online: boolean;

  /**
   * When provided, sets the driver's availability independently of the online flag.
   * Only meaningful when the driver is already online — ignored if going offline
   * (offline always forces available=false).
   */
  @IsOptional()
  @IsBoolean()
  available?: boolean;
}
