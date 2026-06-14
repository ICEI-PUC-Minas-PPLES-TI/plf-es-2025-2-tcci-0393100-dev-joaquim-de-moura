-- CreateEnum
CREATE TYPE "SenderRole" AS ENUM ('PASSENGER', 'DRIVER');

-- AlterEnum: remove REQUESTED from RideStatus
-- Rides with status REQUESTED are converted to PENDING_DRIVER first (safe: REQUESTED was never used in production code)
UPDATE "Ride" SET "status" = 'PENDING_DRIVER'::"RideStatus" WHERE "status" = 'REQUESTED'::"RideStatus";

-- AlterEnum
ALTER TYPE "RideStatus" RENAME TO "RideStatus_old";
CREATE TYPE "RideStatus" AS ENUM ('PENDING_DRIVER', 'ACCEPTED', 'DRIVER_ARRIVING', 'DRIVER_ARRIVED', 'IN_PROGRESS', 'FINISHED', 'CANCELED');
ALTER TABLE "Ride" ALTER COLUMN "status" DROP DEFAULT;
ALTER TABLE "Ride" ALTER COLUMN "status" TYPE "RideStatus" USING ("status"::text::"RideStatus");
ALTER TABLE "Ride" ALTER COLUMN "status" SET DEFAULT 'PENDING_DRIVER'::"RideStatus";
DROP TYPE "RideStatus_old";

-- AlterTable: convert senderRole from String to SenderRole enum
ALTER TABLE "ChatMessage" ALTER COLUMN "senderRole" TYPE "SenderRole" USING ("senderRole"::"SenderRole");

-- CreateIndex
CREATE INDEX "Ride_status_idx" ON "Ride"("status");

-- CreateIndex
CREATE INDEX "Ride_paymentStatus_idx" ON "Ride"("paymentStatus");

-- CreateIndex
CREATE INDEX "Ride_passengerId_createdAt_idx" ON "Ride"("passengerId", "createdAt");

-- CreateIndex
CREATE INDEX "Ride_driverId_status_idx" ON "Ride"("driverId", "status");
