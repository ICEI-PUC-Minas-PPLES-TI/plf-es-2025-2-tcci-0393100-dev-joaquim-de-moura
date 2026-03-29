import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { RideStatus } from '@prisma/client';
import { ConfigService } from '@nestjs/config';
import { GoogleDirectionsService } from '../maps/google-directions.service';

@Injectable()
export class RideService {
  constructor(
    private prisma: PrismaService,
    private config: ConfigService,
    private directions: GoogleDirectionsService,
  ) {}

  // ✅ cálculo estilo Uber (centavos)
  private calcFareCents(params: {
    distanceMeters: number;
    durationSeconds: number;
    baseFareCents: number;
    perKmCents: number;
    perMinuteCents: number;
    minimumFareCents: number;
    bookingFeeCents: number;
    surgeMultiplier: number;
  }) {
    const km = params.distanceMeters / 1000;
    const minutes = params.durationSeconds / 60;

    const variable = params.perKmCents * km + params.perMinuteCents * minutes;
    const raw = params.baseFareCents + params.bookingFeeCents + variable;

    const surged = raw * (params.surgeMultiplier ?? 1);
    const finalCents = Math.max(params.minimumFareCents, Math.round(surged));

    return finalCents;
  }

  // ✅ cria corrida já calculando distância/tempo/preço no backend
  async create(data: {
    passengerId: string;
    originLat: number;
    originLng: number;
    destLat: number;
    destLng: number;
  }) {
    const apiKey = this.config.get<string>('GOOGLE_MAPS_API_KEY');
    if (!apiKey) {
      throw new BadRequestException('GOOGLE_MAPS_API_KEY não configurada no .env');
    }

    // 1) rota (Google Directions)
    const { distanceMeters, durationSeconds } = await this.directions.getRoute(
      data.originLat,
      data.originLng,
      data.destLat,
      data.destLng,
      apiKey,
    );

    // 2) pricing ativo (admin controla por aqui depois)
    const pricing = await this.prisma.pricingConfig.findFirst({
      where: { isActive: true },
      orderBy: { createdAt: 'desc' },
    });

    if (!pricing) {
      throw new BadRequestException('Não existe PricingConfig ativo. Rode o seed ou crie um no painel.');
    }

    // 3) calcula tarifa
    const estimatedFareCents = this.calcFareCents({
      distanceMeters,
      durationSeconds,
      baseFareCents: pricing.baseFareCents,
      perKmCents: pricing.perKmCents,
      perMinuteCents: pricing.perMinuteCents,
      minimumFareCents: pricing.minimumFareCents,
      bookingFeeCents: pricing.bookingFeeCents,
      surgeMultiplier: pricing.surgeMultiplier,
    });

    // 4) salva corrida já com os valores
    return this.prisma.ride.create({
      data: {
        passengerId: data.passengerId,
        driverId: null,

        status: RideStatus.PENDING_DRIVER, // ✅ motorista enxerga aqui

        originLat: data.originLat,
        originLng: data.originLng,
        destLat: data.destLat,
        destLng: data.destLng,

        distanceMeters,
        durationSeconds,
        estimatedFareCents,

        pricingConfigId: pricing.id,
      },
    });
  }

  async findAll() {
    return this.prisma.ride.findMany({ orderBy: { createdAt: 'desc' } });
  }

  // ✅ motorista vê apenas corridas solicitadas (abertas)
  async findOpen() {
    return this.prisma.ride.findMany({
      where: { status: RideStatus.PENDING_DRIVER },
      orderBy: { createdAt: 'desc' },
    });
  }

  // ✅ motorista aceita corrida
  async acceptRide(rideId: string, driverId: string) {
    const ride = await this.prisma.ride.findUnique({ where: { id: rideId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');

    if (ride.status !== RideStatus.PENDING_DRIVER) {
      throw new BadRequestException('Essa corrida não está disponível para aceitar');
    }

    return this.prisma.ride.update({
      where: { id: rideId },
      data: {
        driverId,
        status: RideStatus.ACCEPTED,
      },
    });
  }

  // ✅ atualizar status (IN_PROGRESS, FINISHED, CANCELED)
  async updateStatus(rideId: string, status: RideStatus) {
    const ride = await this.prisma.ride.findUnique({ where: { id: rideId } });
    if (!ride) throw new NotFoundException('Corrida não encontrada');

    return this.prisma.ride.update({
      where: { id: rideId },
      data: { status },
    });
  }
  
  async estimate(data: {
  originLat: number;
  originLng: number;
  destLat: number;
  destLng: number;
}) {
   console.log('📍 ESTIMATE RECEIVED:', data);
    const vals = [
    data.originLat,
    data.originLng,
    data.destLat,
    data.destLng,
  ];

  if (vals.some(v => typeof v !== 'number' || Number.isNaN(v))) {
    console.log('❌ INVALID COORDS:', data);
    throw new BadRequestException('Coordenadas inválidas');
  }
  const apiKey = this.config.get<string>('GOOGLE_MAPS_API_KEY');
  if (!apiKey) {
    throw new BadRequestException('GOOGLE_MAPS_API_KEY não configurada no .env');
  }

  const { distanceMeters, durationSeconds } = await this.directions.getRoute(
    data.originLat,
    data.originLng,
    data.destLat,
    data.destLng,
    apiKey,
  );

  const pricing = await this.prisma.pricingConfig.findFirst({
    where: { isActive: true },
    orderBy: { createdAt: 'desc' },
  });

  if (!pricing) {
    throw new BadRequestException('Não existe PricingConfig ativo.');
  }

  const estimatedFareCents = this.calcFareCents({
    distanceMeters,
    durationSeconds,
    baseFareCents: pricing.baseFareCents,
    perKmCents: pricing.perKmCents,
    perMinuteCents: pricing.perMinuteCents,
    minimumFareCents: pricing.minimumFareCents,
    bookingFeeCents: pricing.bookingFeeCents,
    surgeMultiplier: pricing.surgeMultiplier,
  });

  return {
    distanceMeters,
    durationSeconds,
    estimatedFareCents,
    currency: pricing.currency,
    pricingConfigId: pricing.id,
  };
}
}