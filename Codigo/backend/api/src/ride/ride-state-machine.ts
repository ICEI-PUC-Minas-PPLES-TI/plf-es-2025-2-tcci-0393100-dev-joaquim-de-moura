import { BadRequestException } from '@nestjs/common';
import { RideStatus } from '@prisma/client';

export const ACTIVE_RIDE_STATUSES: RideStatus[] = [
  RideStatus.PENDING_DRIVER,
  RideStatus.ACCEPTED,
  RideStatus.DRIVER_ARRIVING,
  RideStatus.DRIVER_ARRIVED,
  RideStatus.IN_PROGRESS,
];

export const DRIVER_ACTIVE_RIDE_STATUSES: RideStatus[] = [
  RideStatus.ACCEPTED,
  RideStatus.DRIVER_ARRIVING,
  RideStatus.DRIVER_ARRIVED,
  RideStatus.IN_PROGRESS,
];

const allowedTransitions: Record<RideStatus, RideStatus[]> = {
  [RideStatus.PENDING_DRIVER]: [RideStatus.ACCEPTED, RideStatus.CANCELED],
  [RideStatus.ACCEPTED]: [RideStatus.DRIVER_ARRIVING, RideStatus.CANCELED],
  [RideStatus.DRIVER_ARRIVING]: [RideStatus.DRIVER_ARRIVED, RideStatus.CANCELED],
  [RideStatus.DRIVER_ARRIVED]: [RideStatus.IN_PROGRESS, RideStatus.CANCELED],
  [RideStatus.IN_PROGRESS]: [RideStatus.FINISHED, RideStatus.CANCELED],
  [RideStatus.FINISHED]: [],
  [RideStatus.CANCELED]: [],
};

export function assertRideTransition(from: RideStatus, to: RideStatus) {
  if (from === to) return;

  if (!allowedTransitions[from]?.includes(to)) {
    throw new BadRequestException(
      `Transição de corrida inválida: ${from} -> ${to}`,
    );
  }
}
