package br.com.seunome.mobulite.data.remote

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): AuthUser

    @POST("rides")
    suspend fun createRide(@Body body: CreateRideRequest): Ride

    @POST("rides/estimate")
    suspend fun estimateRide(@Body body: EstimateRideRequest): EstimateRideResponse

    // Passageiro acompanha status e localização do motorista
    @GET("rides/{id}")
    suspend fun getRideById(@Path("id") rideId: String): RideStatusResponse

    // Passageiro cancela corrida
    @PATCH("rides/{id}/status")
    suspend fun updateRideStatus(
        @Path("id") rideId: String,
        @Body body: UpdateStatusRequest
    ): RideStatusResponse

    // Histórico de corridas do passageiro
    @GET("rides/my-rides")
    suspend fun getMyRides(): List<RideHistoryItem>

    // Avaliação
    @POST("ratings")
    suspend fun createRating(@Body body: RatingRequest): RatingResponse

    // Recuperação de senha (público)
    @POST("auth/reset-password/request")
    suspend fun requestPasswordReset(@Body body: RequestPasswordResetRequest): VerificationAckResponse

    @POST("auth/reset-password/confirm")
    suspend fun confirmPasswordReset(@Body body: ConfirmPasswordResetRequest): SimpleOkResponse

    // Atualizar nome do perfil (autenticado)
    @PATCH("auth/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): AuthUser

    // Upload foto de perfil do passageiro
    @Multipart
    @POST("auth/upload-photo")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): AuthUser

    // Alterar senha (autenticado)
    @PATCH("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): SimpleOkResponse

    @POST("auth/verification/phone/request")
    suspend fun requestPhoneVerification(): VerificationAckResponse

    @POST("auth/verification/phone/confirm")
    suspend fun confirmPhoneVerification(@Body body: ConfirmCodeBody): SimpleOkResponse

    @PATCH("auth/email")
    suspend fun setEmail(@Body body: SetEmailBody): SimpleOkResponse

    @POST("auth/verification/email/request")
    suspend fun requestEmailVerification(): VerificationAckResponse

    @POST("auth/verification/email/confirm")
    suspend fun confirmEmailVerification(@Body body: ConfirmCodeBody): SimpleOkResponse

    // Motoristas disponíveis no mapa (para passageiro idle)
    @GET("driver/available")
    suspend fun getAvailableDrivers(): List<NearbyDriverLocation>

    // Exclusão de conta
    @DELETE("auth/account")
    suspend fun deleteAccount(): SimpleOkResponse

    // Suporte
    @GET("support/tickets/my")
    suspend fun getMyTickets(): List<SupportTicketResponse>

    @POST("support/tickets")
    suspend fun createTicket(@Body body: CreateTicketRequest): SupportTicketResponse

    // Chat
    @GET("rides/{rideId}/messages")
    suspend fun getChatMessages(@Path("rideId") rideId: String): List<ChatMessage>

    @POST("rides/{rideId}/messages")
    suspend fun sendChatMessage(
        @Path("rideId") rideId: String,
        @Body body: SendMessageRequest
    ): ChatMessage

    // Cupom promocional
    @POST("rides/validate-coupon")
    suspend fun validateCoupon(@Body body: ValidateCouponRequest): CouponValidationResponse

}
