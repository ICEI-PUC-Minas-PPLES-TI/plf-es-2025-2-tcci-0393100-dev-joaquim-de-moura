package br.com.seunome.mobulite.data.remote

data class CreateTicketRequest(
    val subject: String,
    val description: String,
    val type: String? = null,
    val rideId: String? = null
)
