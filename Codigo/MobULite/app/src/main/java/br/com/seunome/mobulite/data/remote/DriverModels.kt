package br.com.seunome.mobulite.data.remote

data class DriverStatusRequest(
    val online: Boolean
)

data class DriverStatusResponse(
    val driverId: String,
    val online: Boolean,
    val message: String? = null
)

data class RideRequestItem(
    val rideId: String,
    val passengerName: String,
    val originAddress: String,
    val destinationAddress: String,
    val price: Double?,
    val distanceMeters: Double?,
    val status: String
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
    val approvalStatus: String,
    val rejectionReason: String?
)

data class PassengerSummary(
    val id: String,
    val name: String?,
    val phone: String
)

data class DriverRideResponse(
    val rideId: String,
    val passengerId: String,
    val passengerName: String?,
    val passengerPhone: String?,
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
    val driverPixQrPayload: String? = null
)