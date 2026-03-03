import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

async function main() {
  const existing = await prisma.pricingConfig.findFirst({
    where: { isActive: true },
  });

  if (!existing) {
    await prisma.pricingConfig.create({
      data: {
        isActive: true,
        baseFareCents: 500,
        perKmCents: 200,
        perMinuteCents: 50,
        minimumFareCents: 800,
        bookingFeeCents: 0,
        surgeMultiplier: 1.0,
        currency: "BRL",
      },
    });
  }
}

main()
  .catch(async (e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });