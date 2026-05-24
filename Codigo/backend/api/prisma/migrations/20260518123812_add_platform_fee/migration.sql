-- AlterTable
ALTER TABLE "PricingConfig" ADD COLUMN     "platformFeePercent" DOUBLE PRECISION NOT NULL DEFAULT 20.0;

-- AlterTable
ALTER TABLE "Ride" ADD COLUMN     "driverReceivableCents" INTEGER,
ADD COLUMN     "platformFeeCents" INTEGER;
