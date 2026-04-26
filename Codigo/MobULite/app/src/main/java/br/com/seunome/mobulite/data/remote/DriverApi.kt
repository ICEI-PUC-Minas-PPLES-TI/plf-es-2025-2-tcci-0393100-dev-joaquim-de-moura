package br.com.seunome.mobulite.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PATCH


interface DriverApi {

    @POST("driver/status")
    suspend fun updateStatus(
        @Body request: DriverStatusRequest
    ): DriverStatusResponse

    @GET("driver/rides/pending")
    suspend fun getPendingRides(): List<RideRequestItem>

    @POST("driver/rides/{rideId}/accept")
    suspend fun acceptRide(
        @Path("rideId") rideId: String
    ): RideActionResponse

    @POST("driver/rides/{rideId}/reject")
    suspend fun rejectRide(
        @Path("rideId") rideId: String
    ): RideActionResponse

    @GET("driver/me")
    suspend fun getDriverMe(): DriverMeResponse

    @GET("driver/rides/current")
    suspend fun getCurrentRide(): DriverRideResponse?

    @PATCH("driver/rides/{rideId}/start")
    suspend fun startRide(
        @Path("rideId") rideId: String
    ): DriverRideResponse

    @PATCH("driver/rides/{rideId}/finish")
    suspend fun finishRide(
        @Path("rideId") rideId: String
    ): DriverRideResponse
}