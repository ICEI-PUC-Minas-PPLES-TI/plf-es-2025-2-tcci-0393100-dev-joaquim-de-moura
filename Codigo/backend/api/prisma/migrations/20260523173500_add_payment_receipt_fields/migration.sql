ALTER TABLE "Ride"
ADD COLUMN "paymentPixPayload" TEXT,
ADD COLUMN "paymentTxId" TEXT,
ADD COLUMN "paymentReceiptNote" TEXT,
ADD COLUMN "paymentReportedAt" TIMESTAMP(3),
ADD COLUMN "paymentConfirmedAt" TIMESTAMP(3),
ADD COLUMN "paymentConfirmedBy" TEXT;

