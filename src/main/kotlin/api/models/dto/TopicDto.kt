package api.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class TopicDto(
    val id: Int,
    val name: String,
    val colorHex: String
)