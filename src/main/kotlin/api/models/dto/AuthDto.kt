package api.models.dto

import api.models.enum.ProviderType
import api.models.enum.UserRole
import io.ktor.network.sockets.SocketAddress
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import java.security.Provider

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class GoogleUserInfo(
    val sub: String? = null,          // google_id (duy nhất, không đổi)
    val name: String,
    val email: String,
    val picture: String? = null
)

@Serializable
data class OAuthUserInfo(
    val provider: ProviderType,
    val sub: String? = null,
    val name: String,
    val email: String,
    val picture: String? = null
)

@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val date_of_birth: String? = null,
    val address: String? = null,
    val role: UserRole,
    var english_certificate_url: String?= null,
    var pedagogical_certificate_url: String? = null,
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
    val id: Int? = null,
    val user: UserResponse? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

@Serializable
data class UserInfo (
    val id: Int,
    val email: String?,
    val name: String,
    val role: UserRole,
    val passwordHash: String? = "",
    val isEmailVerified: Boolean? = false,
)

@Serializable
data class UserResponse(
    val id: Int,
    val email: String? = null,
    val name: String,
    val role: UserRole,
    val isEmailVerified: Boolean? = false,
)

@Serializable
data class OtpRequest (
    val email: String
)

@Serializable
data class OtpVerify (
    val email: String,
    val otp: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val password: String
)

@Serializable
data class OAuthRegisterRequest (
    val provider: ProviderType,
    val name: String,
    val email: String? = null,
    val address: String? = null,
    val role: UserRole,
    var english_certificate_url: String? = null,
    var pedagogical_certificate_url: String? = null
)

@Serializable
data class ChangeEmailRequest (
    val email: String
)

@Serializable
data class ChangePasswordRequest (
    val oldPassword: String,
    val newPassword: String
)