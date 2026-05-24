-- CreateTable
CREATE TABLE "DriverSettlement" (
    "id" TEXT NOT NULL,
    "driverId" TEXT NOT NULL,
    "amountCents" INTEGER NOT NULL,
    "notes" TEXT,
    "method" TEXT NOT NULL DEFAULT 'PIX',
    "settledAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "settledBy" TEXT NOT NULL,

    CONSTRAINT "DriverSettlement_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "DriverSettlement_driverId_idx" ON "DriverSettlement"("driverId");

-- CreateIndex
CREATE INDEX "DriverSettlement_settledAt_idx" ON "DriverSettlement"("settledAt");

-- AddForeignKey
ALTER TABLE "DriverSettlement" ADD CONSTRAINT "DriverSettlement_driverId_fkey" FOREIGN KEY ("driverId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "DriverSettlement" ADD CONSTRAINT "DriverSettlement_settledBy_fkey" FOREIGN KEY ("settledBy") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
