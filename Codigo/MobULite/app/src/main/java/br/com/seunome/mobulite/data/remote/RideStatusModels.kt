package br.com.seunome.mobulite.data.remote

data class CreateRideRequest(
    val originLat: Double,
    val originLng: Double,
    val destLat: Double,
    val destLng: Double,
    val originAddress: String?,
    val destinationAddress: String?
)

data class CreateRideResponse(
    val id: String,
    val status: String
)

data class RideStatusResponse(
    val id: String,
    val status: String,
    val driver: DriverSummary? = null
)

data class DriverSummary(
    val name: String? = null
)