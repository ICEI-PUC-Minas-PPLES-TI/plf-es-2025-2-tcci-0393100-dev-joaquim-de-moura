package br.com.seunome.mobulite.data.remote

data class RegisterRequest(
    val phone: String,
    val name: String?,
    val password: String,
    val role: String?, // "PASSENGER" ou "DRIVER"
    val email: String? = null,
    val cpf: String? = null,
    val birthDate: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val accessibilityNotes: String? = null,
    val acceptedTerms: Boolean? = null,
    val acceptedPrivacy: Boolean? = null
)

data class LoginRequest(
    val phone: String,
    val password: String
)

data class AuthUser(
    val id: String,
    val phone: String,
    val name: String?,
    val role: String,
    val profilePhotoUrl: String? = null,
    val email: String? = null,
    val phoneVerifiedAt: String? = null,
    val emailVerifiedAt: String? = null,
    val blocked: Boolean = false
)

data class AuthResponse(
    val user: AuthUser,
    val accessToken: String
)


data class DriverRegisterRequest(
    val phone: String,
    val password: String,
    val name: String,
    val cnhImageUrl: String?,
    val cnhNumber: String?,
    val cnhCategory: String?,
    val hasEar: Boolean
)

data class DriverRegisterResponse(
    val user: RegisteredUser,
    val accessToken: String,
    val message: String? = null
)

data class RegisteredUser(
    val id: String,
    val phone: String,
    val name: String?,
    val role: String,
    val createdAt: String
)

data class RegisterResponse(
    val user: RegisteredUser,
    val accessToken: String
)

data class RequestPasswordResetRequest(
    val phone: String
)

data class ConfirmPasswordResetRequest(
    val phone: String,
    val code: String,
    val newPassword: String
)

data class UpdateProfileRequest(
    val name: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class SimpleOkResponse(
    val ok: Boolean,
    val message: String
)
