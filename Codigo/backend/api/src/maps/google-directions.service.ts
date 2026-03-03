import { Injectable } from '@nestjs/common';
import axios from 'axios';

type DirectionsResult = {
  distanceMeters: number;
  durationSeconds: number;
};

@Injectable()
export class GoogleDirectionsService {
  async getRoute(originLat: number, originLng: number, destLat: number, destLng: number, apiKey: string): Promise<DirectionsResult> {
    const url =
      `https://maps.googleapis.com/maps/api/directions/json` +
      `?origin=${originLat},${originLng}` +
      `&destination=${destLat},${destLng}` +
      `&mode=driving` +
      `&key=${encodeURIComponent(apiKey)}`;

    const { data } = await axios.get(url);

    if (data.status !== 'OK' || !data.routes?.length) {
      throw new Error(`Directions error: ${data.status} ${data.error_message ?? ''}`.trim());
    }

    const leg = data.routes[0].legs?.[0];
    if (!leg?.distance?.value || !leg?.duration?.value) {
      throw new Error('Directions returned no distance/duration');
    }

    return {
      distanceMeters: leg.distance.value,
      durationSeconds: leg.duration.value,
    };
  }
}