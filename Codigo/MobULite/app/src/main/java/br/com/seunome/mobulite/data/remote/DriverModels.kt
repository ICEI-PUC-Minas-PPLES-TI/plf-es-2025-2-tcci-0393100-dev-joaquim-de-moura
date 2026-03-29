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