package api.models.dto

import api.models.enum.UserRole
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val role: UserRole,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class AuthResult (
    val ok: Boolean,
    val message: String? = "",
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

@Serializable
data class UserInfo (
    val id: Int,
    val email: String,
    val name: String,
    val role: UserRole,
    val passwordHash: String? = ""
)