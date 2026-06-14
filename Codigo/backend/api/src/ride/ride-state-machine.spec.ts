import { BadRequestException } from '@nestjs/common';
import { RideStatus } from '@prisma/client';
import { assertRideTransition, ACTIVE_RIDE_STATUSES, DRIVER_ACTIVE_RIDE_STATUSES } from './ride-state-machine';

describe('ride-state-machine', () => {
  describe('assertRideTransition', () => {
    it('deve permitir PENDING_DRIVER -> ACCEPTED', () => {
      expect(() =>
        assertRideTransition(RideStatus.PENDING_DRIVER, RideStatus.ACCEPTED),
      ).not.toThrow();
    });

    it('deve permitir ACCEPTED -> DRIVER_ARRIVING', () => {
      expect(() =>
        assertRideTransition(RideStatus.ACCEPTED, RideStatus.DRIVER_ARRIVING),
      ).not.toThrow();
    });

    it('deve permitir DRIVER_ARRIVING -> DRIVER_ARRIVED', () => {
      expect(() =>
        assertRideTransition(RideStatus.DRIVER_ARRIVING, RideStatus.DRIVER_ARRIVED),
      ).not.toThrow();
    });

    it('deve permitir DRIVER_ARRIVED -> IN_PROGRESS', () => {
      expect(() =>
        assertRideTransition(RideStatus.DRIVER_ARRIVED, RideStatus.IN_PROGRESS),
      ).not.toThrow();
    });

    it('deve permitir IN_PROGRESS -> FINISHED (TA5)', () => {
      expect(() =>
        assertRideTransition(RideStatus.IN_PROGRESS, RideStatus.FINISHED),
      ).not.toThrow();
    });

    it('deve permitir cancelamento em PENDING_DRIVER', () => {
      expect(() =>
        assertRideTransition(RideStatus.PENDING_DRIVER, RideStatus.CANCELED),
      ).not.toThrow();
    });

    it('deve permitir cancelamento em ACCEPTED', () => {
      expect(() =>
        assertRideTransition(RideStatus.ACCEPTED, RideStatus.CANCELED),
      ).not.toThrow();
    });

    it('não deve lançar exceção quando from === to', () => {
      expect(() =>
        assertRideTransition(RideStatus.IN_PROGRESS, RideStatus.IN_PROGRESS),
      ).not.toThrow();
    });

    it('deve bloquear transição inválida: PENDING_DRIVER -> IN_PROGRESS', () => {
      expect(() =>
        assertRideTransition(RideStatus.PENDING_DRIVER, RideStatus.IN_PROGRESS),
      ).toThrow(BadRequestException);
    });

    it('deve bloquear transição de FINISHED para qualquer estado', () => {
      expect(() =>
        assertRideTransition(RideStatus.FINISHED, RideStatus.CANCELED),
      ).toThrow(BadRequestException);
    });

    it('deve bloquear transição de CANCELED para qualquer estado', () => {
      expect(() =>
        assertRideTransition(RideStatus.CANCELED, RideStatus.PENDING_DRIVER),
      ).toThrow(BadRequestException);
    });

    it('deve bloquear transição retroativa: DRIVER_ARRIVING -> ACCEPTED', () => {
      expect(() =>
        assertRideTransition(RideStatus.DRIVER_ARRIVING, RideStatus.ACCEPTED),
      ).toThrow(BadRequestException);
    });
  });

  describe('ACTIVE_RIDE_STATUSES', () => {
    it('deve incluir PENDING_DRIVER, ACCEPTED, DRIVER_ARRIVING, DRIVER_ARRIVED e IN_PROGRESS', () => {
      expect(ACTIVE_RIDE_STATUSES).toEqual(
        expect.arrayContaining([
          RideStatus.PENDING_DRIVER,
          RideStatus.ACCEPTED,
          RideStatus.DRIVER_ARRIVING,
          RideStatus.DRIVER_ARRIVED,
          RideStatus.IN_PROGRESS,
        ]),
      );
    });

    it('não deve incluir FINISHED ou CANCELED', () => {
      expect(ACTIVE_RIDE_STATUSES).not.toContain(RideStatus.FINISHED);
      expect(ACTIVE_RIDE_STATUSES).not.toContain(RideStatus.CANCELED);
    });
  });

  describe('DRIVER_ACTIVE_RIDE_STATUSES', () => {
    it('não deve incluir PENDING_DRIVER', () => {
      expect(DRIVER_ACTIVE_RIDE_STATUSES).not.toContain(RideStatus.PENDING_DRIVER);
    });
  });
});
