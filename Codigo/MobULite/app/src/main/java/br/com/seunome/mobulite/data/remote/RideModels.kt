package br.com.seunome.mobulite.data.remote

data class AcceptRideRequest(
    val driverId: String
)

data class UpdateStatusRequest(
    val status: String
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