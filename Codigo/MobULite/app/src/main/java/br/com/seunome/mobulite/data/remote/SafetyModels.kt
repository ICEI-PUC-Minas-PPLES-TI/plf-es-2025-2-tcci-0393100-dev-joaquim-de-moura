package br.com.seunome.mobulite.data.remote

import com.google.gson.JsonElement

data class VerificationAckResponse(
    val ok: Boolean? = null,
    val message: String? = null,
    val channel: String? = null,
    val destination: String? = null,
    val expiresAt: String? = null,
    val devHint: String? = null,
)

data class ConfirmCodeBody(val code: String)

data class SetEmailBody(val email: String)

data class SafetyShareBody(val channel: String? = null)

data class SafetyEmergencyBody(
    val note: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

data class SafetyAudioBody(
    val phase: String,
    val consentAccepted: Boolean? = null,
    val durationMs: Long? = null,
)

data class RideAuditEntry(
    val id: String,
    val action: String,
    val metadata: JsonElement? = null,
    val createdAt: String,
    val actorUserId: String? = null,
)
