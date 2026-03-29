-- CreateEnum
CREATE TYPE "DriverApprovalStatus" AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- AlterTable
ALTER TABLE "DriverProfile" ADD COLUMN     "approvalStatus" "DriverApprovalStatus" NOT NULL DEFAULT 'PENDING',
ADD COLUMN     "approvedAt" TIMESTAMP(3),
ADD COLUMN     "cnhCategory" TEXT,
ADD COLUMN     "cnhImageUrl" TEXT,
ADD COLUMN     "cnhNumber" TEXT,
ADD COLUMN     "hasEar" BOOLEAN,
ADD COLUMN     "rejectionReason" TEXT;
