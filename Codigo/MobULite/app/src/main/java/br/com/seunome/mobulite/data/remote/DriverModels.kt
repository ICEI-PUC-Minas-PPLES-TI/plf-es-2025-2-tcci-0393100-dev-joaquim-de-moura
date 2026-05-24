package br.com.seunome.mobulite.data.remote

data class DriverStatusRequest(val online: Boolean, val available: Boolean? = null)

data class DriverStatusResponse(
    val driverId: String,
    val online: Boolean,
    val available: Boolean? = null,
    val message: String? = null
)

data class LocationUpdateResponse(val ok: Boolean)

data class RideRequestItem(
    val rideId: String,
    val passengerName: String,
    val passengerPhone: String?,
    val passengerPhotoUrl: String? = null,
    val originAddress: String?,
    val destinationAddress: String?,
    val originLat: Double?,
    val originLng: Double?,
    val destLat: Double?,
    val destLng: Double?,
    val price: Double?,
    val distanceMeters: Double?,
    val durationSeconds: Double?,
    val paymentMethod: String?,
    val status: String,
    val createdAt: String?,
    val passengerTrips: Int? = null,
    val pickupDistanceMeters: Double? = null
)

data class RideActionResponse(
    val success: Boolean,
    val message: String
)

data class DriverMeResponse(
    val id: String,
    val name: String?,
    val phone: String,
    val online: Boolean,
    val available: Boolean? = null,
    val approvalStatus: String,
    val rejectionReason: String?,
    val profilePhotoUrl: String? = null,
    val cnhNumber: String? = null,
    val cnhCategory: String? = null,
    val cnhExpiresAt: String? = null,
    val cnhImageUrl: String? = null,
    val hasEar: Boolean? = null,
    val cpf: String? = null,
    val pixKey: String? = null,
    val pixQrCodeUrl: String? = null,
    val pixQrPayload: String? = null,
    val vehicleModel: String? = null,
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val vehicleYear: Int? = null,
    val vehicleCapacity: Int? = null
)

data class DriverRideResponse(
    val rideId: String,
    val passengerId: String,
    val passengerName: String?,
    val passengerPhone: String?,
    val passengerPhotoUrl: String? = null,
    val originLat: Double,
    val originLng: Double,
    val destLat: Double,
    val destLng: Double,
    val originAddress: String?,
    val destinationAddress: String?,
    val price: Int?,
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val status: String,
    val driverPixQrPayload: String? = null,
    val paymentMethod: String? = null,
    val paymentStatus: String? = null,
    val paymentPixPayload: String? = null,
    val paymentTxId: String? = null
)

data class ConfirmPaymentRequest(
    val txId: String? = null,
    val receiptNote: String? = null
)

data class PaymentConfirmationResponse(
    val ok: Boolean,
    val rideId: String,
    val paymentStatus: String,
    val paymentConfirmedAt: String? = null,
    val paymentReportedAt: String? = null,
    val paymentTxId: String? = null
)

data class RideHistoryItem(
    val id: String,
    val status: String,
    val originAddress: String?,
    val destinationAddress: String?,
    val estimatedFareCents: Int?,
    val distanceMeters: Int?,
    val driverName: String?,
    val rating: Int?,
    val createdAt: String
)

data class DriverRideHistoryItem(
    val id: String,
    val status: String,
    val originAddress: String?,
    val destinationAddress: String?,
    val estimatedFareCents: Int?,
    val platformFeeCents: Int? = null,
    val driverReceivableCents: Int? = null,
    val distanceMeters: Int?,
    val durationSeconds: Int?,
    val passengerName: String?,
    val passengerPhone: String?,
    val ratingScore: Int?,
    val ratingComment: String?,
    val createdAt: String
)

data class DriverRideHistoryResponse(
    val totalEarned: Int,
    val totalReceivable: Int = 0,
    val totalPlatformFee: Int = 0,
    val todayEarned: Int = 0,
    val todayReceivable: Int = 0,
    val weekEarned: Int = 0,
    val weekReceivable: Int = 0,
    val monthEarned: Int = 0,
    val monthReceivable: Int = 0,
    val acceptedCount: Int = 0,
    val todayCount: Int = 0,
    val rejectedCount: Int = 0,
    val rides: List<DriverRideHistoryItem>
)

data class DriverProfileUpdateRequest(
    val name: String? = null,
    val cnhNumber: String? = null,
    val cnhCategory: String? = null,
    val cnhExpiresAt: String? = null,
    val hasEar: Boolean? = null,
    val pixQrPayload: String? = null,
    val pixKey: String? = null,
    val pixQrCodeUrl: String? = null,
    val vehicleModel: String? = null,
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val cpf: String? = null,
    val vehicleYear: Int? = null,
    val vehicleCapacity: Int? = null
)

data class DriverProfileResponse(
    val id: String,
    val name: String?,
    val phone: String,
    val cnhNumber: String?,
    val cnhCategory: String?,
    val cnhExpiresAt: String? = null,
    val hasEar: Boolean?,
    val cpf: String? = null,
    val pixKey: String?,
    val pixQrCodeUrl: String?,
    val pixQrPayload: String?,
    val vehicleModel: String?,
    val vehiclePlate: String?,
    val vehicleColor: String?,
    val vehicleYear: Int? = null,
    val vehicleCapacity: Int? = null,
    val approvalStatus: String
)

data class CnhUploadResponse(val cnhImageUrl: String)

data class NearbyDriverLocation(val id: String, val lat: Double, val lng: Double)

data class DriverRatingSummary(
    val ratings: List<RatingItem>,
    val average: Double?,
    val total: Int
)

data class RatingItem(
    val id: String,
    val rideId: String,
    val score: Int,
    val comment: String?,
    val createdAt: String
)

data class DriverSettlementItem(
    val id: String,
    val amountCents: Int,
    val notes: String?,
    val method: String,
    val settledAt: String
)

data class BillingCycle(
    val weekStart: String,
    val weekEnd: String,
    val totalFeeCents: Int,
    val totalGrossCents: Int,
    val totalReceivableCents: Int,
    val totalDistanceMeters: Int,
    val rideCount: Int,
    val status: String,         // OPEN | PENDING_PAYMENT | PARTIAL | OVERDUE | PAID
    val paidCents: Int,
    val balanceCents: Int,
    val isCurrentWeek: Boolean,
    val daysElapsed: Int,
    val daysTotal: Int
)

data class BillingCyclesResponse(
    val cycles: List<BillingCycle>,
    val currentWeekStart: String,
    val platformPixKey: String?,
    val totalFeeCents: Int,
    val totalSettledCents: Int,
    val pendingCount: Int,
    val hasPendingRequest: Boolean,
    val pendingRequest: PendingRequestInfo?
)

data class PendingRequestInfo(
    val id: String,
    val amountCents: Int,
    val requestedAt: String
)

data class DriverPaymentRequestItem(
    val id: String,
    val amountCents: Int,
    val status: String,
    val notes: String? = null,
    val receiptUrl: String? = null,
    val requestedAt: String = "",
    val rejectionReason: String? = null,
    val reviewedAt: String? = null
)

data class ReceiptUploadResponse(val id: String, val receiptUrl: String?)

data class CreatePaymentRequestBody(val amountCents: Int, val notes: String)

data class FcmTokenRequest(val token: String)
data class FcmTokenResponse(val ok: Boolean)

data class DriverBalanceResponse(
    val totalFeeCents: Int,
    val totalSettledCents: Int,
    val balanceCents: Int,
    val limitCents: Int = 5000,
    val isBlocked: Boolean = false,
    val platformPixKey: String? = null,
    val settlements: List<DriverSettlementItem>,
    val paymentRequests: List<DriverPaymentRequestItem> = emptyList()
)
