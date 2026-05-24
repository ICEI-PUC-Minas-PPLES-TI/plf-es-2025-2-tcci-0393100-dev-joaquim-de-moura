package br.com.seunome.mobulite.data.remote

import okhttp3.MultipartBody
import retrofit2.http.*

interface DriverApi {

    @POST("driver/status")
    suspend fun updateStatus(@Body request: DriverStatusRequest): DriverStatusResponse

    @PATCH("driver/location")
    suspend fun updateLocation(@Body request: DriverLocationRequest): LocationUpdateResponse

    @GET("driver/rides/pending")
    suspend fun getPendingRides(): List<RideRequestItem>

    @POST("driver/rides/{rideId}/accept")
    suspend fun acceptRide(@Path("rideId") rideId: String): RideActionResponse

    @POST("driver/rides/{rideId}/reject")
    suspend fun rejectRide(@Path("rideId") rideId: String): RideActionResponse

    @GET("driver/me")
    suspend fun getDriverMe(): DriverMeResponse

    @GET("driver/rides/current")
    suspend fun getCurrentRide(): DriverRideResponse?

    @PATCH("driver/rides/{rideId}/arriving")
    suspend fun markArriving(@Path("rideId") rideId: String): DriverRideResponse

    @PATCH("driver/rides/{rideId}/arrived")
    suspend fun markArrived(@Path("rideId") rideId: String): DriverRideResponse

    @PATCH("driver/rides/{rideId}/start")
    suspend fun startRide(@Path("rideId") rideId: String): DriverRideResponse

    @PATCH("driver/rides/{rideId}/finish")
    suspend fun finishRide(@Path("rideId") rideId: String): DriverRideResponse

    @PATCH("payments/rides/{rideId}/status")
    suspend fun updatePaymentStatus(
        @Path("rideId") rideId: String,
        @Body body: Map<String, String>
    ): okhttp3.ResponseBody

    @POST("payments/rides/{rideId}/confirm")
    suspend fun confirmPayment(
        @Path("rideId") rideId: String,
        @Body body: ConfirmPaymentRequest = ConfirmPaymentRequest()
    ): PaymentConfirmationResponse

    @POST("payments/rides/{rideId}/not-received")
    suspend fun markPaymentNotReceived(
        @Path("rideId") rideId: String,
        @Body body: ConfirmPaymentRequest = ConfirmPaymentRequest()
    ): PaymentConfirmationResponse

    @PATCH("driver/profile")
    suspend fun updateProfile(@Body request: DriverProfileUpdateRequest): DriverProfileResponse

    @GET("driver/rides/history")
    suspend fun getRideHistory(): DriverRideHistoryResponse

    @PATCH("driver/rides/{rideId}/cancel")
    suspend fun cancelRide(@Path("rideId") rideId: String): RideActionResponse

    @GET("ratings/me")
    suspend fun getMyRatings(): DriverRatingSummary

    @Multipart
    @POST("driver/upload-photo")
    suspend fun uploadDriverPhoto(@Part photo: MultipartBody.Part): DriverMeResponse

    @Multipart
    @POST("driver/upload-cnh")
    suspend fun uploadCnhPhoto(@Part photo: MultipartBody.Part): CnhUploadResponse

    @GET("driver/financial/balance")
    suspend fun getFinancialBalance(): DriverBalanceResponse

    @POST("driver/financial/payment-request")
    suspend fun createPaymentRequest(@Body body: CreatePaymentRequestBody): DriverPaymentRequestItem

    @GET("driver/financial/payment-requests")
    suspend fun getPaymentRequests(): List<DriverPaymentRequestItem>

    @Multipart
    @POST("driver/financial/payment-requests/{id}/receipt")
    suspend fun uploadPaymentReceipt(
        @Path("id") id: String,
        @Part receipt: MultipartBody.Part
    ): ReceiptUploadResponse

    @GET("driver/billing/cycles")
    suspend fun getBillingCycles(): BillingCyclesResponse

    // Chat (acesso pelo motorista)
    @GET("rides/{rideId}/messages")
    suspend fun getChatMessages(@Path("rideId") rideId: String): List<ChatMessage>

    @POST("rides/{rideId}/messages")
    suspend fun sendChatMessage(
        @Path("rideId") rideId: String,
        @Body body: SendMessageRequest
    ): ChatMessage

}
