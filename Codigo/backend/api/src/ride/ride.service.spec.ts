import { Test, TestingModule } from '@nestjs/testing';
import { ConfigService } from '@nestjs/config';
import { GoogleDirectionsService } from '../maps/google-directions.service';
import { PrismaService } from '../prisma/prisma.service';
import { RealtimeService } from '../realtime/realtime.service';
import { NotificationService } from '../notification/notification.service';
import { RideService } from './ride.service';

describe('RideService', () => {
  let service: RideService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RideService,
        {
          provide: PrismaService,
          useValue: {},
        },
        {
          provide: ConfigService,
          useValue: {},
        },
        {
          provide: GoogleDirectionsService,
          useValue: {},
        },
        {
          provide: RealtimeService,
          useValue: {
            emitToDrivers: jest.fn(),
            emitAdmin: jest.fn(),
            emitRide: jest.fn(),
            emitToUser: jest.fn(),
          },
        },
        {
          provide: NotificationService,
          useValue: {
            sendToUser: jest.fn(),
            sendToUsers: jest.fn(),
          },
        },
      ],
    }).compile();

    service = module.get<RideService>(RideService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
