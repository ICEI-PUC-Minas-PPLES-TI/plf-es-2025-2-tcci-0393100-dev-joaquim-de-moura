import { Module } from '@nestjs/common';
import { GoogleDirectionsService } from './google-directions.service';

@Module({
  providers: [GoogleDirectionsService],
  exports: [GoogleDirectionsService],
})
export class MapsModule {}