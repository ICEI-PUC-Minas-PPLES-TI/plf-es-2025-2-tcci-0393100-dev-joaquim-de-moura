ALTER TYPE "RideStatus" ADD VALUE IF NOT EXISTS 'REQUESTED';
ALTER TYPE "RideStatus" ADD VALUE IF NOT EXISTS 'DRIVER_ARRIVING';
ALTER TYPE "RideStatus" ADD VALUE IF NOT EXISTS 'DRIVER_ARRIVED';

CREATE TYPE "PaymentMethod" AS ENUM ('CASH', 'PIX');
CREATE TYPE "PaymentStatus" AS ENUM ('PENDING', 'RECEIVED', 'NOT_RECEIVED', 'CANCELED');

ALTER TABLE "DriverProfile"
ADD COLUMN     "pixKey" TEXT,
ADD COLUMN     "pixQrCodeUrl" TEXT,
ADD COLUMN     "vehicleModel" TEXT,
ADD COLUMN     "vehiclePlate" TEXT,
ADD COLUMN     "vehicleColor" TEXT;

ALTER TABLE "Ride"
ADD COLUMN     "paymentMethod" "PaymentMethod" NOT NULL DEFAULT 'CASH',
ADD COLUMN     "paymentStatus" "PaymentStatus" NOT NULL DEFAULT 'PENDING',
ADD COLUMN     "acceptedAt" TIMESTAMP(3),
ADD COLUMN     "driverArrivingAt" TIMESTAMP(3),
ADD COLUMN     "driverArrivedAt" TIMESTAMP(3),
ADD COLUMN     "startedAt" TIMESTAMP(3),
ADD COLUMN     "finishedAt" TIMESTAMP(3),
ADD COLUMN     "canceledAt" TIMESTAMP(3);

UPDATE "Ride"
SET "paymentStatus" = 'CANCELED'
WHERE "status" = 'CANCELED';

UPDATE "Ride"
SET "acceptedAt" = "updatedAt"
WHERE "status" IN ('ACCEPTED', 'IN_PROGRESS', 'FINISHED');

UPDATE "Ride"
SET "startedAt" = "updatedAt"
WHERE "status" IN ('IN_PROGRESS', 'FINISHED');

UPDATE "Ride"
SET "finishedAt" = "updatedAt"
WHERE "status" = 'FINISHED';

UPDATE "Ride"
SET "canceledAt" = "updatedAt"
WHERE "status" = 'CANCELED';
