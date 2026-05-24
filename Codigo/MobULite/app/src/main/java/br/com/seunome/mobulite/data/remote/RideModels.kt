package br.com.seunome.mobulite.data.remote

data class AcceptRideRequest(
    val driverId: String
)

data class UpdateStatusRequest(
    val status: String,
    val cancelReason: String? = null
)

data class Ride(
    val id: String,
    val passengerId: String,
    val driverId: String?,
    val originLat: Double,
    val originLng: Double,
    val destLat: Double,
    val destLng: Double,
    val status: String,
    val createdAt: String
)

// ─── Chat ─────────────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val rideId: String,
    val senderId: String,
    val senderRole: String, // "PASSENGER" | "DRIVER"
    val content: String,
    val sentAt: String,
    val readAt: String? = null
)

data class SendMessageRequest(
    val content: String
)

// ─── Coupon ───────────────────────────────────────────────────────────────────

data class ValidateCouponRequest(val code: String)

data class CouponValidationResponse(
    val valid: Boolean,
    val code: String,
    val discountPercent: Int? = null,
    val discountCents: Int? = null,
    val description: String? = null
)


