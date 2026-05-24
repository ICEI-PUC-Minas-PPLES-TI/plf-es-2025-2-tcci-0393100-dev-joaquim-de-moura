package br.com.seunome.mobulite.data.remote

data class EstimateRideRequest(
    val originLat: Double,
    val originLng: Double,
    val destLat: Double,
    val destLng: Double
)

data class EstimateRideResponse(
    val distanceMeters: Int,
    val durationSeconds: Int,
    val estimatedFareCents: Int,
    val encodedPolyline: String? = null,
    val currency: String? = "BRL"
)