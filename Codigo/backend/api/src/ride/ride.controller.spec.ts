import { Test, TestingModule } from '@nestjs/testing';
import { RideController } from './ride.controller';
import { RideService } from './ride.service';

describe('RideController', () => {
  let controller: RideController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [RideController],
      providers: [
        {
          provide: RideService,
          useValue: {},
        },
      ],
    }).compile();

    controller = module.get<RideController>(RideController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
