package api.models.dto


import api.utils.DateSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.Date

@Serializable
data class UpdateProfileRequest (
    val id: Int,
    val fullName: String,
    @Serializable(with = DateSerializer::class)
    val DoB: Date,
    val address: String
)
@Serializable
data class GetProfileResponse  (
    val id: Int,
    val fullName: String?,
    @Serializable(with = DateSerializer::class)
    val DoB: Date,
    val address: String?,
    val avatarUrl: String?,
    val email: String?,
)

@Serializable
data class GetLanguageResponse (
    val language: String
)
