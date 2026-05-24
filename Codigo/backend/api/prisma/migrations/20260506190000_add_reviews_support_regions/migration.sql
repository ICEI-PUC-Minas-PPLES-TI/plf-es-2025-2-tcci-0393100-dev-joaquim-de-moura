CREATE TYPE "SupportTicketType" AS ENUM (
  'PAYMENT',
  'RIDE_CANCELLATION',
  'SAFETY',
  'APP_ISSUE',
  'OTHER'
);

CREATE TYPE "SupportTicketStatus" AS ENUM (
  'OPEN',
  'IN_REVIEW',
  'RESOLVED',
  'CLOSED'
);

CREATE TABLE "OperationRegion" (
  "id" TEXT NOT NULL,
  "name" TEXT NOT NULL,
  "city" TEXT,
  "active" BOOLEAN NOT NULL DEFAULT true,
  "centerLat" DOUBLE PRECISION,
  "centerLng" DOUBLE PRECISION,
  "radiusMeters" INTEGER,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,

  CONSTRAINT "OperationRegion_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "Review" (
  "id" TEXT NOT NULL,
  "rideId" TEXT NOT NULL,
  "passengerId" TEXT NOT NULL,
  "driverId" TEXT NOT NULL,
  "rating" INTEGER NOT NULL,
  "comment" TEXT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT "Review_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "SupportTicket" (
  "id" TEXT NOT NULL,
  "creatorId" TEXT NOT NULL,
  "rideId" TEXT,
  "type" "SupportTicketType" NOT NULL DEFAULT 'OTHER',
  "status" "SupportTicketStatus" NOT NULL DEFAULT 'OPEN',
  "subject" TEXT NOT NULL,
  "description" TEXT NOT NULL,
  "resolution" TEXT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  "closedAt" TIMESTAMP(3),

  CONSTRAINT "SupportTicket_pkey" PRIMARY KEY ("id")
);

ALTER TABLE "PricingConfig"
ADD COLUMN "name" TEXT,
ADD COLUMN "regionId" TEXT;

CREATE UNIQUE INDEX "Review_rideId_key" ON "Review"("rideId");
CREATE INDEX "Review_driverId_idx" ON "Review"("driverId");
CREATE INDEX "Review_passengerId_idx" ON "Review"("passengerId");
CREATE INDEX "SupportTicket_creatorId_idx" ON "SupportTicket"("creatorId");
CREATE INDEX "SupportTicket_rideId_idx" ON "SupportTicket"("rideId");
CREATE INDEX "SupportTicket_status_idx" ON "SupportTicket"("status");
CREATE INDEX "PricingConfig_isActive_idx" ON "PricingConfig"("isActive");
CREATE INDEX "PricingConfig_regionId_isActive_idx" ON "PricingConfig"("regionId", "isActive");

ALTER TABLE "Review"
ADD CONSTRAINT "Review_rideId_fkey" FOREIGN KEY ("rideId") REFERENCES "Ride"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "Review"
ADD CONSTRAINT "Review_passengerId_fkey" FOREIGN KEY ("passengerId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "Review"
ADD CONSTRAINT "Review_driverId_fkey" FOREIGN KEY ("driverId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "SupportTicket"
ADD CONSTRAINT "SupportTicket_creatorId_fkey" FOREIGN KEY ("creatorId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE "SupportTicket"
ADD CONSTRAINT "SupportTicket_rideId_fkey" FOREIGN KEY ("rideId") REFERENCES "Ride"("id") ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE "PricingConfig"
ADD CONSTRAINT "PricingConfig_regionId_fkey" FOREIGN KEY ("regionId") REFERENCES "OperationRegion"("id") ON DELETE SET NULL ON UPDATE CASCADE;
