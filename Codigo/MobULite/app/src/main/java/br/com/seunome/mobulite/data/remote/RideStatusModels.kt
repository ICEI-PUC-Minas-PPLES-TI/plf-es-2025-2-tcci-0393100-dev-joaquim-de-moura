package br.com.seunome.mobulite.data.remote

data class CreateRideRequest(
    val originLat: Double,
    val originLng: Double,
    val destLat: Double,
    val destLng: Double,
    val originAddress: String?,
    val destinationAddress: String?,
    val paymentMethod: String = "CASH",
    val promoCode: String? = null
)

data class CreateRideResponse(
    val id: String,
    val status: String
)

data class RideStatusResponse(
    val id: String,
    val status: String,
    val estimatedFareCents: Int? = null,
    val distanceMeters: Int? = null,
    val durationSeconds: Int? = null,
    val originAddress: String? = null,
    val destinationAddress: String? = null,
    val paymentMethod: String? = null,
    val paymentStatus: String? = null,
    val paymentPixPayload: String? = null,
    val paymentTxId: String? = null,
    val paymentConfirmedAt: String? = null,
    val driver: DriverLocationSummary? = null,
    val hasRating: Boolean = false
)

data class DriverLocationSummary(
    val name: String? = null,
    val phone: String? = null,
    val photoUrl: String? = null,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val vehicleModel: String? = null,
    val vehiclePlate: String? = null,
    val vehicleColor: String? = null,
    val averageRating: Float? = null,
    val pixKey: String? = null,
)

data class RatingRequest(
    val rideId: String,
    val score: Int,
    val comment: String? = null
)

data class RatingResponse(
    val id: String,
    val rideId: String,
    val score: Int,
    val comment: String? = null
)

data class DriverLocationRequest(
    val lat: Double,
    val lng: Double
)
