/*
  Warnings:

  - Added the required column `updatedAt` to the `Ride` table without a default value. This is not possible if the table is not empty.

*/
-- AlterEnum
ALTER TYPE "RideStatus" ADD VALUE 'OPEN';

-- AlterTable
ALTER TABLE "Ride" ADD COLUMN     "distanceMeters" INTEGER,
ADD COLUMN     "durationSeconds" INTEGER,
ADD COLUMN     "estimatedFareCents" INTEGER,
ADD COLUMN     "pricingConfigId" TEXT,
ADD COLUMN     "updatedAt" TIMESTAMP(3) NOT NULL,
ALTER COLUMN "status" DROP DEFAULT;

-- CreateTable
CREATE TABLE "PricingConfig" (
    "id" TEXT NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "baseFareCents" INTEGER NOT NULL DEFAULT 500,
    "perKmCents" INTEGER NOT NULL DEFAULT 200,
    "perMinuteCents" INTEGER NOT NULL DEFAULT 50,
    "minimumFareCents" INTEGER NOT NULL DEFAULT 800,
    "bookingFeeCents" INTEGER NOT NULL DEFAULT 0,
    "surgeMultiplier" DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    "currency" TEXT NOT NULL DEFAULT 'BRL',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "PricingConfig_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "Ride" ADD CONSTRAINT "Ride_pricingConfigId_fkey" FOREIGN KEY ("pricingConfigId") REFERENCES "PricingConfig"("id") ON DELETE SET NULL ON UPDATE CASCADE;
