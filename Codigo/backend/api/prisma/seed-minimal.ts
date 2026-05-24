import {
  PrismaClient,
  UserRole,
  DriverApprovalStatus,
} from '@prisma/client';

const prisma = new PrismaClient();
const HASH = '$2b$10$HBmEElnN7MD8t2IbP/CpbOF4FT7dbHiHN7czASBVl49QtEu0H5Y/m'; // Senha@123

async function resetDatabase() {
  await prisma.driverPaymentRequest.deleteMany();
  await prisma.driverSettlement.deleteMany();
  await prisma.rideAuditLog.deleteMany();
  await prisma.review.deleteMany();
  await prisma.rideRejection.deleteMany();
  await prisma.supportTicket.deleteMany();
  await prisma.ride.deleteMany();
  await prisma.driverProfile.deleteMany();
  await prisma.verificationChallenge.deleteMany();
  await prisma.user.deleteMany();
  await prisma.pricingConfig.deleteMany();
  await prisma.operationRegion.deleteMany();
}

async function main() {
  console.log('🗑️  Limpando banco de dados...');
  await resetDatabase();
  console.log('✅ Banco limpo.\n');

  const region = await prisma.operationRegion.create({
    data: {
      name: 'Pará de Minas',
      city: 'Pará de Minas',
      active: true,
      centerLat: -19.868,
      centerLng: -44.614,
      radiusMeters: 10000,
    },
  });

  await prisma.pricingConfig.create({
    data: {
      isActive: true,
      name: 'Tarifa padrão',
      regionId: region.id,
      baseFareCents: 500,
      perKmCents: 200,
      perMinuteCents: 50,
      minimumFareCents: 800,
      bookingFeeCents: 0,
      surgeMultiplier: 1.0,
      currency: 'BRL',
    },
  });

  await prisma.user.create({
    data: {
      phone: '31999000000',
      email: 'admin@mobu.com.br',
      name: 'Admin MobU',
      role: UserRole.ADMIN,
      passwordHash: HASH,
      phoneVerifiedAt: new Date(),
      emailVerifiedAt: new Date(),
    },
  });

  await prisma.user.create({
    data: {
      phone: '31991111111',
      email: 'joao@email.com',
      name: 'João Silva',
      role: UserRole.PASSENGER,
      passwordHash: HASH,
      phoneVerifiedAt: new Date(),
    },
  });

  const carlosUser = await prisma.user.create({
    data: {
      phone: '31993333333',
      email: 'carlos@email.com',
      name: 'Carlos Santos',
      role: UserRole.DRIVER,
      passwordHash: HASH,
      phoneVerifiedAt: new Date(),
    },
  });

  await prisma.driverProfile.create({
    data: {
      userId: carlosUser.id,
      online: true,
      available: true,
      approvalStatus: DriverApprovalStatus.APPROVED,
      approvedAt: new Date('2025-01-10'),
      cnhNumber: '12345678901',
      cnhCategory: 'B',
      cnhExpiresAt: new Date('2030-06-15'),
      hasEar: true,
      cpf: '123.456.789-00',
      vehicleModel: 'Fiat Uno',
      vehiclePlate: 'ABC1D23',
      vehicleColor: 'Branco',
      vehicleYear: 2019,
      vehicleCapacity: 4,
      pixKey: '31993333333',
      currentLat: -19.8654,
      currentLng: -44.6122,
    },
  });

  console.log('✅ Seed mínimo concluído!\n');
  console.log('── Credenciais (senha: Senha@123) ───────────────────────────');
  console.log('  Admin:      31999000000  admin@mobu.com.br');
  console.log('  Passageiro: 31991111111  joao@email.com   – João Silva');
  console.log('  Motorista:  31993333333  carlos@email.com – Carlos Santos (APROVADO, ONLINE)');
}

main()
  .catch(async (e) => {
    console.error('❌ Erro:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
