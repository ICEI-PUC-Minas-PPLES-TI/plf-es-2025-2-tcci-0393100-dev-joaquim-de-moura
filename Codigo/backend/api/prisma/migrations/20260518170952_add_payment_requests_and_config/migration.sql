-- CreateEnum
CREATE TYPE "PaymentRequestStatus" AS ENUM ('PENDING', 'CONFIRMED', 'REJECTED');

-- CreateTable
CREATE TABLE "DriverPaymentRequest" (
    "id" TEXT NOT NULL,
    "driverId" TEXT NOT NULL,
    "amountCents" INTEGER NOT NULL,
    "status" "PaymentRequestStatus" NOT NULL DEFAULT 'PENDING',
    "notes" TEXT,
    "requestedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "reviewedAt" TIMESTAMP(3),
    "reviewedBy" TEXT,
    "rejectionReason" TEXT,

    CONSTRAINT "DriverPaymentRequest_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "SystemConfig" (
    "key" TEXT NOT NULL,
    "value" TEXT NOT NULL,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "SystemConfig_pkey" PRIMARY KEY ("key")
);

-- AlterTable
ALTER TABLE "DriverSettlement" ADD COLUMN "paymentRequestId" TEXT;

-- CreateIndex
CREATE INDEX "DriverPaymentRequest_driverId_idx" ON "DriverPaymentRequest"("driverId");
CREATE INDEX "DriverPaymentRequest_status_idx" ON "DriverPaymentRequest"("status");
CREATE INDEX "DriverPaymentRequest_requestedAt_idx" ON "DriverPaymentRequest"("requestedAt");

-- UniqueIndex
CREATE UNIQUE INDEX "DriverSettlement_paymentRequestId_key" ON "DriverSettlement"("paymentRequestId");

-- AddForeignKey
ALTER TABLE "DriverSettlement" ADD CONSTRAINT "DriverSettlement_paymentRequestId_fkey" FOREIGN KEY ("paymentRequestId") REFERENCES "DriverPaymentRequest"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "DriverPaymentRequest" ADD CONSTRAINT "DriverPaymentRequest_driverId_fkey" FOREIGN KEY ("driverId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "DriverPaymentRequest" ADD CONSTRAINT "DriverPaymentRequest_reviewedBy_fkey" FOREIGN KEY ("reviewedBy") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- SeedDefaults
INSERT INTO "SystemConfig" ("key", "value", "updatedAt") VALUES
  ('DRIVER_DEBT_LIMIT_CENTS', '5000', NOW()),
  ('PLATFORM_PIX_KEY', 'plataforma@mobu.com.br', NOW())
ON CONFLICT ("key") DO NOTHING;
