package br.com.seunome.mobulite.data.remote

import retrofit2.http.*

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): AuthUser

    @GET("rides/open")
    suspend fun getOpenRides(): List<Ride>

    @POST("rides")
    suspend fun createRide(@Body body: CreateRideRequest): Ride

    @POST("rides/estimate")
    suspend fun estimateRide(@Body body: EstimateRideRequest): EstimateRideResponse

    @POST("rides/{id}/accept")
    suspend fun acceptRide(
        @Path("id") id: String,
        @Body body: AcceptRideRequest
    ): Ride

    @PATCH("rides/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: UpdateStatusRequest
    ): Ride
}