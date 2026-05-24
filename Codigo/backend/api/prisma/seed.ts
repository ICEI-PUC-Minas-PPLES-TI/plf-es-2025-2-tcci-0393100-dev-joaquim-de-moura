import {
  PrismaClient,
  UserRole,
  DriverApprovalStatus,
  RideStatus,
  PaymentMethod,
  PaymentStatus,
  SupportTicketType,
  SupportTicketStatus,
} from '@prisma/client';

const prisma = new PrismaClient();
const HASH = '$2b$10$HBmEElnN7MD8t2IbP/CpbOF4FT7dbHiHN7czASBVl49QtEu0H5Y/m'; // Senha@123

// Pará de Minas, MG
const LOCS = {
  pracaCentral:  { lat: -19.8684, lng: -44.6142, addr: 'Praça Padre Belchior, Centro, Pará de Minas - MG' },
  hospital:      { lat: -19.8730, lng: -44.6185, addr: 'Hospital Regional de Pará de Minas - MG' },
  terminal:      { lat: -19.8710, lng: -44.6165, addr: 'Terminal Rodoviário, Pará de Minas - MG' },
  supermercado:  { lat: -19.8652, lng: -44.6110, addr: 'Supermercado BH, Av. Getúlio Vargas, Pará de Minas - MG' },
  escola:        { lat: -19.8760, lng: -44.6220, addr: 'Escola Estadual João XXIII, Pará de Minas - MG' },
  jardim:        { lat: -19.8620, lng: -44.6070, addr: 'Bairro Jardim das Flores, Pará de Minas - MG' },
  forumJustica:  { lat: -19.8695, lng: -44.6158, addr: 'Fórum da Justiça, Centro, Pará de Minas - MG' },
  policia:       { lat: -19.8668, lng: -44.6130, addr: 'Delegacia de Polícia, Pará de Minas - MG' },
  unimed:        { lat: -19.8741, lng: -44.6200, addr: 'Unimed Pará de Minas - MG' },
  prefeitura:    { lat: -19.8679, lng: -44.6148, addr: 'Prefeitura Municipal de Pará de Minas - MG' },
  parque:        { lat: -19.8600, lng: -44.6050, addr: 'Parque Municipal, Pará de Minas - MG' },
  aeroporto:     { lat: -19.8550, lng: -44.6000, addr: 'Aeródromo Municipal, Pará de Minas - MG' },
};

function pastDate(daysAgo: number, hoursAgo = 0) {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  d.setHours(d.getHours() - hoursAgo);
  return d;
}

async function resetDatabase() {
  await prisma.chatMessage.deleteMany();
  await prisma.rideAuditLog.deleteMany();
  await prisma.review.deleteMany();
  await prisma.rideRejection.deleteMany();
  await prisma.supportTicket.deleteMany();
  await prisma.driverSettlement.deleteMany();
  await prisma.driverPaymentRequest.deleteMany();
  await prisma.ride.deleteMany();
  await prisma.driverProfile.deleteMany();
  await prisma.verificationChallenge.deleteMany();
  await prisma.promoCode.deleteMany();
  await prisma.user.deleteMany();
  await prisma.pricingConfig.deleteMany();
  await prisma.operationRegion.deleteMany();
}

async function main() {
  console.log('🗑️  Limpando banco de dados...');
  await resetDatabase();
  console.log('✅ Banco limpo.\n');

  // ── Região e preços ────────────────────────────────────────────────────────
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

  const pricing = await prisma.pricingConfig.create({
    data: {
      isActive: true,
      name: 'Tarifa padrão – Pará de Minas',
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

  // ── Admin ──────────────────────────────────────────────────────────────────
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

  // ── Passageiros ────────────────────────────────────────────────────────────
  const [joao, maria, lucas, fernanda, rafael, beatriz] = await Promise.all([
    prisma.user.create({ data: { phone: '31991111111', email: 'joao@email.com',     name: 'João Silva',      role: UserRole.PASSENGER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31992222222', email: 'maria@email.com',    name: 'Maria Oliveira',  role: UserRole.PASSENGER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31996666666', email: 'lucas@email.com',    name: 'Lucas Ferreira',  role: UserRole.PASSENGER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31997777777', email: 'fernanda@email.com', name: 'Fernanda Lima',   role: UserRole.PASSENGER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31998888888', email: 'rafael@email.com',   name: 'Rafael Souza',    role: UserRole.PASSENGER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31990000001', email: 'beatriz@email.com',  name: 'Beatriz Mendes',  role: UserRole.PASSENGER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
  ]);

  // ── Motoristas ─────────────────────────────────────────────────────────────
  const [carlosUser, anaUser, brunoUser, julUser, pedroUser, tiUser] = await Promise.all([
    prisma.user.create({ data: { phone: '31993333333', email: 'carlos@email.com',  name: 'Carlos Santos',  role: UserRole.DRIVER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31994444444', email: 'ana@email.com',     name: 'Ana Costa',      role: UserRole.DRIVER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31990000002', email: 'bruno@email.com',   name: 'Bruno Martins',  role: UserRole.DRIVER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31990000003', email: 'juliana@email.com', name: 'Juliana Rocha',  role: UserRole.DRIVER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31995555555', email: 'pedro@email.com',   name: 'Pedro Almeida',  role: UserRole.DRIVER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
    prisma.user.create({ data: { phone: '31990000004', email: 'tiago@email.com',   name: 'Tiago Ribeiro',  role: UserRole.DRIVER, passwordHash: HASH, phoneVerifiedAt: new Date() } }),
  ]);

  // Perfis dos motoristas
  const [carlos, ana, bruno] = await Promise.all([
    prisma.driverProfile.create({ data: {
      userId: carlosUser.id, online: true, available: true,
      approvalStatus: DriverApprovalStatus.APPROVED, approvedAt: new Date('2025-01-10'),
      cnhNumber: '12345678901', cnhCategory: 'B', cnhExpiresAt: new Date('2030-06-15'),
      hasEar: true, cpf: '123.456.789-00',
      vehicleModel: 'Fiat Uno', vehiclePlate: 'ABC1D23', vehicleColor: 'Branco', vehicleYear: 2019, vehicleCapacity: 4,
      pixKey: '31993333333',
      currentLat: LOCS.pracaCentral.lat + 0.003, currentLng: LOCS.pracaCentral.lng + 0.002,
    }}),
    prisma.driverProfile.create({ data: {
      userId: anaUser.id, online: true, available: true,
      approvalStatus: DriverApprovalStatus.APPROVED, approvedAt: new Date('2025-02-20'),
      cnhNumber: '98765432100', cnhCategory: 'B', cnhExpiresAt: new Date('2028-11-30'),
      hasEar: false, cpf: '987.654.321-00',
      vehicleModel: 'Honda Fit', vehiclePlate: 'DEF5G67', vehicleColor: 'Prata', vehicleYear: 2021, vehicleCapacity: 4,
      pixKey: 'ana.costa@email.com',
      currentLat: LOCS.jardim.lat, currentLng: LOCS.jardim.lng,
    }}),
    prisma.driverProfile.create({ data: {
      userId: brunoUser.id, online: false, available: false,
      approvalStatus: DriverApprovalStatus.APPROVED, approvedAt: new Date('2025-03-05'),
      cnhNumber: '55566677788', cnhCategory: 'B', cnhExpiresAt: new Date('2029-04-20'),
      hasEar: true, cpf: '555.666.777-88',
      vehicleModel: 'Toyota Corolla', vehiclePlate: 'MNO3P45', vehicleColor: 'Preto', vehicleYear: 2022, vehicleCapacity: 4,
      pixKey: 'bruno.martins@email.com',
      currentLat: LOCS.terminal.lat, currentLng: LOCS.terminal.lng,
    }}),
    prisma.driverProfile.create({ data: {
      userId: julUser.id, online: false, available: false,
      approvalStatus: DriverApprovalStatus.REJECTED,
      rejectionReason: 'CNH vencida e documentos ilegíveis enviados.',
      cnhNumber: '11100099988', cnhCategory: 'B',
      cpf: '111.000.999-88',
      vehicleModel: 'Chevrolet Onix', vehiclePlate: 'PQR6Q78', vehicleColor: 'Vermelho', vehicleYear: 2017, vehicleCapacity: 4,
      pixKey: '31990000003',
    }}),
    prisma.driverProfile.create({ data: {
      userId: pedroUser.id, online: false, available: false,
      approvalStatus: DriverApprovalStatus.PENDING,
      cnhNumber: '11122233344', cnhCategory: 'B', cpf: '111.222.333-44',
      vehicleModel: 'Volkswagen Gol', vehiclePlate: 'GHI8H90', vehicleColor: 'Cinza', vehicleYear: 2018, vehicleCapacity: 4,
      pixKey: '31995555555',
    }}),
    prisma.driverProfile.create({ data: {
      userId: tiUser.id, online: false, available: false,
      approvalStatus: DriverApprovalStatus.PENDING,
      cnhNumber: '22233344455', cnhCategory: 'B', cpf: '222.333.444-55',
      vehicleModel: 'Renault Kwid', vehiclePlate: 'STU9S01', vehicleColor: 'Azul', vehicleYear: 2023, vehicleCapacity: 4,
      pixKey: 'tiago.ribeiro@email.com',
    }}),
  ]);

  // ── Corridas finalizadas ───────────────────────────────────────────────────
  type RideInput = {
    passengerId: string; driverId: string;
    origin: typeof LOCS.pracaCentral; dest: typeof LOCS.hospital;
    dist: number; dur: number; fare: number;
    method: PaymentMethod; createdAgo: number;
    rating: number; comment: string;
  };

  const finishedRides: RideInput[] = [
    { passengerId: joao.id,     driverId: carlosUser.id, origin: LOCS.pracaCentral, dest: LOCS.hospital,     dist: 1800, dur: 390,  fare: 1160, method: PaymentMethod.CASH, createdAgo: 30, rating: 5, comment: 'Motorista muito educado e pontual!' },
    { passengerId: maria.id,    driverId: anaUser.id,    origin: LOCS.supermercado, dest: LOCS.escola,       dist: 2500, dur: 540,  fare: 1450, method: PaymentMethod.PIX,  createdAgo: 25, rating: 4, comment: 'Boa viagem, carro bem limpo.' },
    { passengerId: joao.id,     driverId: anaUser.id,    origin: LOCS.terminal,     dest: LOCS.jardim,       dist: 3100, dur: 660,  fare: 1720, method: PaymentMethod.CASH, createdAgo: 20, rating: 5, comment: 'Excelente! Muito recomendado.' },
    { passengerId: lucas.id,    driverId: carlosUser.id, origin: LOCS.jardim,       dest: LOCS.pracaCentral, dist: 2900, dur: 620,  fare: 1680, method: PaymentMethod.PIX,  createdAgo: 18, rating: 3, comment: 'Viagem ok, mas motorista atrasou.' },
    { passengerId: fernanda.id, driverId: brunoUser.id,  origin: LOCS.hospital,     dest: LOCS.supermercado, dist: 1600, dur: 340,  fare: 1020, method: PaymentMethod.CASH, createdAgo: 15, rating: 5, comment: 'Perfeito! Carro confortável.' },
    { passengerId: rafael.id,   driverId: anaUser.id,    origin: LOCS.escola,       dest: LOCS.terminal,     dist: 2200, dur: 480,  fare: 1340, method: PaymentMethod.PIX,  createdAgo: 12, rating: 4, comment: 'Bom serviço.' },
    { passengerId: beatriz.id,  driverId: carlosUser.id, origin: LOCS.forumJustica, dest: LOCS.unimed,       dist: 1400, dur: 300,  fare: 980,  method: PaymentMethod.CASH, createdAgo: 10, rating: 5, comment: 'Chegou rápido, ótima experiência!' },
    { passengerId: joao.id,     driverId: brunoUser.id,  origin: LOCS.prefeitura,   dest: LOCS.parque,       dist: 1200, dur: 260,  fare: 840,  method: PaymentMethod.PIX,  createdAgo: 8,  rating: 4, comment: 'Tranquilo e seguro.' },
    { passengerId: maria.id,    driverId: carlosUser.id, origin: LOCS.policia,      dest: LOCS.jardim,       dist: 2700, dur: 580,  fare: 1580, method: PaymentMethod.CASH, createdAgo: 6,  rating: 5, comment: 'Pontual e educado. Nota máxima!' },
    { passengerId: lucas.id,    driverId: brunoUser.id,  origin: LOCS.terminal,     dest: LOCS.hospital,     dist: 2000, dur: 430,  fare: 1260, method: PaymentMethod.PIX,  createdAgo: 5,  rating: 2, comment: 'Rota mais longa do que o necessário.' },
    { passengerId: fernanda.id, driverId: anaUser.id,    origin: LOCS.pracaCentral, dest: LOCS.aeroporto,    dist: 4100, dur: 840,  fare: 2120, method: PaymentMethod.PIX,  createdAgo: 4,  rating: 5, comment: 'Ótima motorista! Super recomendo.' },
    { passengerId: rafael.id,   driverId: carlosUser.id, origin: LOCS.unimed,       dest: LOCS.prefeitura,   dist: 900,  dur: 200,  fare: 800,  method: PaymentMethod.CASH, createdAgo: 3,  rating: 4, comment: 'Viagem curta e tranquila.' },
    { passengerId: beatriz.id,  driverId: brunoUser.id,  origin: LOCS.escola,       dest: LOCS.policia,      dist: 1700, dur: 360,  fare: 1080, method: PaymentMethod.CASH, createdAgo: 2,  rating: 5, comment: 'Excelente como sempre!' },
    { passengerId: joao.id,     driverId: anaUser.id,    origin: LOCS.parque,       dest: LOCS.hospital,     dist: 3300, dur: 700,  fare: 1860, method: PaymentMethod.PIX,  createdAgo: 1,  rating: 3, comment: 'Ok, sem problemas.' },
    { passengerId: lucas.id,    driverId: carlosUser.id, origin: LOCS.aeroporto,    dest: LOCS.pracaCentral, dist: 4500, dur: 920,  fare: 2360, method: PaymentMethod.PIX,  createdAgo: 1,  rating: 5, comment: 'Veio me buscar no aeródromo! Perfeito.' },
  ];

  for (const r of finishedRides) {
    const rideDate = pastDate(r.createdAgo);
    const ride = await prisma.ride.create({
      data: {
        passengerId: r.passengerId,
        driverId: r.driverId,
        status: RideStatus.FINISHED,
        originLat: r.origin.lat, originLng: r.origin.lng, originAddress: r.origin.addr,
        destLat: r.dest.lat,     destLng: r.dest.lng,     destinationAddress: r.dest.addr,
        distanceMeters: r.dist,
        durationSeconds: r.dur,
        estimatedFareCents: r.fare,
        paymentMethod: r.method,
        paymentStatus: PaymentStatus.RECEIVED,
        pricingConfigId: pricing.id,
        createdAt: rideDate,
        acceptedAt: new Date(rideDate.getTime() + 2 * 60000),
        startedAt:  new Date(rideDate.getTime() + 6 * 60000),
        finishedAt: new Date(rideDate.getTime() + (6 + Math.round(r.dur / 60)) * 60000),
      },
    });

    await prisma.review.create({
      data: {
        rideId: ride.id,
        passengerId: r.passengerId,
        driverId: r.driverId,
        rating: r.rating,
        comment: r.comment,
      },
    });
  }

  // ── Corridas canceladas ────────────────────────────────────────────────────
  const canceledRides = [
    { passengerId: maria.id,    origin: LOCS.escola,       dest: LOCS.pracaCentral, dist: 2100, dur: 480, fare: 1220, daysAgo: 14 },
    { passengerId: rafael.id,   origin: LOCS.terminal,     dest: LOCS.jardim,       dist: 2800, dur: 600, fare: 1660, daysAgo: 9  },
    { passengerId: beatriz.id,  origin: LOCS.supermercado, dest: LOCS.hospital,     dist: 1900, dur: 410, fare: 1180, daysAgo: 7  },
    { passengerId: lucas.id,    origin: LOCS.pracaCentral, dest: LOCS.aeroporto,    dist: 4200, dur: 860, fare: 2180, daysAgo: 3  },
  ];

  for (const r of canceledRides) {
    const d = pastDate(r.daysAgo);
    await prisma.ride.create({
      data: {
        passengerId: r.passengerId,
        status: RideStatus.CANCELED,
        originLat: r.origin.lat, originLng: r.origin.lng, originAddress: r.origin.addr,
        destLat: r.dest.lat,     destLng: r.dest.lng,     destinationAddress: r.dest.addr,
        distanceMeters: r.dist, durationSeconds: r.dur, estimatedFareCents: r.fare,
        paymentMethod: PaymentMethod.CASH,
        paymentStatus: PaymentStatus.CANCELED,
        pricingConfigId: pricing.id,
        createdAt: d, canceledAt: new Date(d.getTime() + 3 * 60000),
      },
    });
  }

  // ── Corrida aguardando motorista (PENDING_DRIVER) ──────────────────────────
  await prisma.ride.create({
    data: {
      passengerId: fernanda.id,
      status: RideStatus.PENDING_DRIVER,
      originLat: LOCS.prefeitura.lat, originLng: LOCS.prefeitura.lng, originAddress: LOCS.prefeitura.addr,
      destLat: LOCS.unimed.lat,       destLng: LOCS.unimed.lng,       destinationAddress: LOCS.unimed.addr,
      distanceMeters: 1100, durationSeconds: 240, estimatedFareCents: 880,
      paymentMethod: PaymentMethod.PIX,
      paymentStatus: PaymentStatus.PENDING,
      pricingConfigId: pricing.id,
      createdAt: pastDate(0, 0),
    },
  });

  // ── Tickets de suporte ─────────────────────────────────────────────────────
  await Promise.all([
    prisma.supportTicket.create({ data: {
      creatorId: joao.id, type: SupportTicketType.OTHER, status: SupportTicketStatus.OPEN,
      subject: 'Dúvida sobre cobrança',
      description: 'O valor cobrado foi diferente do estimado. Gostaria de entender o motivo.',
    }}),
    prisma.supportTicket.create({ data: {
      creatorId: lucas.id, type: SupportTicketType.PAYMENT, status: SupportTicketStatus.IN_REVIEW,
      subject: 'Pagamento via Pix não confirmado',
      description: 'Realizei o pagamento via Pix mas o motorista disse que não recebeu.',
    }}),
    prisma.supportTicket.create({ data: {
      creatorId: rafael.id, type: SupportTicketType.RIDE_CANCELLATION, status: SupportTicketStatus.RESOLVED,
      subject: 'Cobrança indevida após cancelamento',
      description: 'A corrida foi cancelada dentro do prazo mas fui cobrado mesmo assim.',
      resolution: 'Verificamos e confirmamos que não houve cobrança. Possivelmente confusão com outra transação.',
    }}),
    prisma.supportTicket.create({ data: {
      creatorId: fernanda.id, type: SupportTicketType.APP_ISSUE, status: SupportTicketStatus.OPEN,
      subject: 'Aplicativo travando ao solicitar corrida',
      description: 'Toda vez que clico em "Solicitar corrida" o app fecha sozinho.',
    }}),
    prisma.supportTicket.create({ data: {
      creatorId: beatriz.id, type: SupportTicketType.SAFETY, status: SupportTicketStatus.IN_REVIEW,
      subject: 'Motorista com comportamento inadequado',
      description: 'O motorista fez comentários inapropriados durante a viagem. Quero registrar formalmente.',
    }}),
  ]);

  // ── Resumo ─────────────────────────────────────────────────────────────────
  console.log('✅ Seed concluído!\n');
  console.log('── Credenciais (senha: Senha@123) ───────────────────────────');
  console.log('  Admin:        31999000000  admin@mobu.com.br');
  console.log('  Passageiro 1: 31991111111  joao@email.com      – João Silva');
  console.log('  Passageiro 2: 31992222222  maria@email.com     – Maria Oliveira');
  console.log('  Passageiro 3: 31996666666  lucas@email.com     – Lucas Ferreira');
  console.log('  Passageiro 4: 31997777777  fernanda@email.com  – Fernanda Lima');
  console.log('  Passageiro 5: 31998888888  rafael@email.com    – Rafael Souza');
  console.log('  Passageiro 6: 31990000001  beatriz@email.com   – Beatriz Mendes');
  console.log('  Motorista 1:  31993333333  carlos@email.com    – Carlos Santos  (APROVADO, ONLINE)');
  console.log('  Motorista 2:  31994444444  ana@email.com       – Ana Costa      (APROVADO, ONLINE)');
  console.log('  Motorista 3:  31990000002  bruno@email.com     – Bruno Martins  (APROVADO, OFFLINE)');
  console.log('  Motorista 4:  31990000003  juliana@email.com   – Juliana Rocha  (REJEITADO)');
  console.log('  Motorista 5:  31995555555  pedro@email.com     – Pedro Almeida  (PENDENTE)');
  console.log('  Motorista 6:  31990000004  tiago@email.com     – Tiago Ribeiro  (PENDENTE)');
  console.log('\n── Dados criados ─────────────────────────────────────────────');
  console.log('  15 corridas FINISHED com avaliações (⭐ 2–5)');
  console.log('   4 corridas CANCELED');
  console.log('   1 corrida PENDING_DRIVER (aguardando motorista)');
  console.log('   5 tickets de suporte (OPEN / IN_REVIEW / RESOLVED)');
}

main()
  .catch(async (e) => {
    console.error('❌ Erro no seed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
