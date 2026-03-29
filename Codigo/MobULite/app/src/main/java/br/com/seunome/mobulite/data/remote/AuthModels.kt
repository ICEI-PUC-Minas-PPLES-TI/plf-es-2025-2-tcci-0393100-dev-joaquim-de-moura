package br.com.seunome.mobulite.data.remote

data class RegisterRequest(
    val phone: String,
    val name: String?,
    val password: String,
    val role: String? // "PASSENGER" ou "DRIVER"
)

data class LoginRequest(
    val phone: String,
    val password: String
)

data class AuthUser(
    val id: String,
    val phone: String,
    val name: String?,
    val role: String
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