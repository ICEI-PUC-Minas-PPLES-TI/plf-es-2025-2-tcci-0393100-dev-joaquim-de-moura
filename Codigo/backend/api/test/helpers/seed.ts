import { PrismaService } from '../../src/prisma/prisma.service';

// Hash bcrypt pré-computado para "Senha@123" (custo 10)
const PASSWORD_HASH =
  '$2b$10$HBmEElnN7MD8t2IbP/CpbOF4FT7dbHiHN7czASBVl49QtEu0H5Y/m';

// Centro de Pará de Minas — MG
export const PARA_DE_MINAS = {
  lat: -19.8684,
  lng: -44.6142,
  addr: 'Praça Padre Belchior, Centro, Pará de Minas - MG',
};

export const HOSPITAL = {
  lat: -19.873,
  lng: -44.6185,
  addr: 'Hospital Regional de Pará de Minas - MG',
};

export interface SeededPassenger {
  id: string;
  phone: string;
  password: string;
}

export interface SeededDriver {
  id: string;
  phone: string;
  password: string;
  profileId: string;
}

export interface SeededAdmin {
  id: string;
  phone: string;
  password: string;
}

export interface SeededRegion {
  regionId: string;
  pricingConfigId: string;
}

/**
 * Cria uma região de operação ativa com configuração de preço.
 * Necessária para que corridas dentro de Pará de Minas sejam aceitas pela API.
 */
export async function seedRegion(prisma: PrismaService): Promise<SeededRegion> {
  const region = await prisma.operationRegion.create({
    data: {
      name: 'Pará de Minas - Teste',
      city: 'Pará de Minas',
      active: true,
      centerLat: PARA_DE_MINAS.lat,
      centerLng: PARA_DE_MINAS.lng,
      radiusMeters: 15_000,
    },
  });

  const pricingConfig = await prisma.pricingConfig.create({
    data: {
      name: 'Tarifa Teste',
      isActive: true,
      baseFareCents: 500,
      perKmCents: 200,
      perMinuteCents: 50,
      minimumFareCents: 800,
      platformFeePercent: 20,
      regionId: region.id,
    },
  });

  return { regionId: region.id, pricingConfigId: pricingConfig.id };
}

/**
 * Cria um usuário passageiro QA com perfil completo.
 * O phone deve ser único por suite de testes.
 */
export async function seedPassenger(
  prisma: PrismaService,
  phone: string,
): Promise<SeededPassenger> {
  const user = await prisma.user.create({
    data: {
      phone,
      name: 'Passageiro QA',
      role: 'PASSENGER',
      passwordHash: PASSWORD_HASH,
      passengerProfile: {
        create: {
          cpf: phone.replace(/\D/g, '').slice(-11).padStart(11, '0'),
          birthDate: new Date('1995-06-15'),
          termsAcceptedAt: new Date(),
          privacyAcceptedAt: new Date(),
        },
      },
    },
  });

  return { id: user.id, phone, password: 'Senha@123' };
}

/**
 * Cria um usuário motorista QA já aprovado, online e disponível,
 * com localização no centro de Pará de Minas.
 */
export async function seedDriver(
  prisma: PrismaService,
  phone: string,
): Promise<SeededDriver> {
  const user = await prisma.user.create({
    data: {
      phone,
      name: 'Motorista QA',
      role: 'DRIVER',
      passwordHash: PASSWORD_HASH,
      driverProfile: {
        create: {
          approvalStatus: 'APPROVED',
          approvedAt: new Date(),
          online: true,
          available: true,
          currentLat: PARA_DE_MINAS.lat,
          currentLng: PARA_DE_MINAS.lng,
          cnhNumber: '00000000001',
          cnhCategory: 'B',
          hasEar: false,
          vehicleModel: 'Gol',
          vehiclePlate: 'QAT0001',
          vehicleColor: 'Prata',
          vehicleYear: 2020,
          vehicleCapacity: 4,
          pixKey: phone,
          pixQrPayload: `00020101021226580014BR.GOV.BCB.PIX0136${phone}5204000053039865802BR5913MotoristaTeste6013PARA DE MINAS62140510TESTEPIX016304ABCD`,
        },
      },
    },
    include: { driverProfile: true },
  });

  return {
    id: user.id,
    phone,
    password: 'Senha@123',
    profileId: user.driverProfile!.id,
  };
}

/**
 * Cria um motorista QA com status PENDING (aguardando aprovação do admin).
 */
export async function seedPendingDriver(
  prisma: PrismaService,
  phone: string,
): Promise<SeededDriver> {
  const user = await prisma.user.create({
    data: {
      phone,
      name: 'Motorista Pendente QA',
      role: 'DRIVER',
      passwordHash: PASSWORD_HASH,
      driverProfile: {
        create: {
          approvalStatus: 'PENDING',
          online: false,
          available: false,
          cnhNumber: '99999999901',
          cnhCategory: 'B',
          hasEar: false,
          vehicleModel: 'Uno',
          vehiclePlate: 'QAP0001',
          vehicleColor: 'Branco',
          vehicleYear: 2019,
          vehicleCapacity: 4,
          pixKey: phone,
          pixQrPayload: null,
        },
      },
    },
    include: { driverProfile: true },
  });

  return {
    id: user.id,
    phone,
    password: 'Senha@123',
    profileId: user.driverProfile!.id,
  };
}

/**
 * Cria um usuário administrador QA.
 */
export async function seedAdmin(
  prisma: PrismaService,
  phone: string,
): Promise<SeededAdmin> {
  const user = await prisma.user.create({
    data: {
      phone,
      name: 'Admin QA',
      role: 'ADMIN',
      passwordHash: PASSWORD_HASH,
    },
  });

  return { id: user.id, phone, password: 'Senha@123' };
}

/**
 * Remove todos os dados criados pelas seeds de teste.
 * Passa os IDs de usuário para deletar apenas os dados desta suite.
 */
export async function cleanupUsers(
  prisma: PrismaService,
  userIds: string[],
): Promise<void> {
  if (userIds.length === 0) return;

  await prisma.chatMessage.deleteMany({
    where: { ride: { passengerId: { in: userIds } } },
  });
  await prisma.rideAuditLog.deleteMany({
    where: { ride: { passengerId: { in: userIds } } },
  });
  await prisma.review.deleteMany({
    where: { passengerId: { in: userIds } },
  });
  await prisma.rideRejection.deleteMany({
    where: { ride: { passengerId: { in: userIds } } },
  });
  await prisma.supportTicket.deleteMany({
    where: { creatorId: { in: userIds } },
  });
  await prisma.driverSettlement.deleteMany({
    where: { driverId: { in: userIds } },
  });
  await prisma.driverPaymentRequest.deleteMany({
    where: { driverId: { in: userIds } },
  });
  await prisma.ride.deleteMany({
    where: { passengerId: { in: userIds } },
  });
  await prisma.passengerProfile.deleteMany({
    where: { userId: { in: userIds } },
  });
  await prisma.verificationChallenge.deleteMany({
    where: { userId: { in: userIds } },
  });
  await prisma.driverProfile.deleteMany({
    where: { userId: { in: userIds } },
  });
  await prisma.user.deleteMany({
    where: { id: { in: userIds } },
  });
}

/**
 * Remove regiões de operação e configurações de preço criadas para testes.
 */
export async function cleanupRegion(
  prisma: PrismaService,
  regionId: string,
): Promise<void> {
  await prisma.pricingConfig.deleteMany({ where: { regionId } });
  await prisma.operationRegion.delete({ where: { id: regionId } });
}
