-- AlterTable
ALTER TABLE "DriverProfile" ADD COLUMN     "cnhExpiresAt" TIMESTAMP(3),
ADD COLUMN     "cpf" TEXT,
ADD COLUMN     "vehicleCapacity" INTEGER,
ADD COLUMN     "vehicleYear" INTEGER;

-- AlterTable
ALTER TABLE "User" ADD COLUMN     "blocked" BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN     "blockedAt" TIMESTAMP(3);
