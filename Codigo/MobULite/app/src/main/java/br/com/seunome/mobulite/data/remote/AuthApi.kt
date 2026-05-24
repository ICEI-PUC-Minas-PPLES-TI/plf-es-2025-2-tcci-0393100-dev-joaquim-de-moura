package br.com.seunome.mobulite.data.remote

import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/register-driver")
    suspend fun registerDriver(
        @Body request: DriverRegisterRequest
    ): DriverRegisterResponse

    @PATCH("auth/fcm-token")
    suspend fun saveFcmToken(@Body body: FcmTokenRequest): FcmTokenResponse
}