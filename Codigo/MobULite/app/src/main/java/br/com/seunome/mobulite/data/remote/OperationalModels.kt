package br.com.seunome.mobulite.data.remote

data class ReviewRequest(
    val rating: Int,
    val comment: String? = null
)

data class ReviewResponse(
    val id: String,
    val rideId: String,
    val rating: Int,
    val comment: String? = null,
    val createdAt: String? = null
)

data class SupportTicketRequest(
    val rideId: String? = null,
    val type: String = "OTHER",
    val subject: String,
    val description: String
)

data class SupportTicketResponse(
    val id: String,
    val type: String,
    val status: String,
    val subject: String,
    val description: String,
    val resolution: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class DriverEarningsResponse(
    val totalRides: Int,
    val totalEstimatedFareCents: Int,
    val monthRides: Int,
    val monthEstimatedFareCents: Int,
    val pendingPaymentRides: Int,
    val pendingPaymentCents: Int,
    val receivedPaymentRides: Int,
    val receivedPaymentCents: Int,
    val averageRating: Double? = null,
    val totalReviews: Int
)
