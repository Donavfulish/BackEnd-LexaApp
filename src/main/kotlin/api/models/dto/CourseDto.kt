package api.models.dto

import kotlinx.serialization.Serializable

// Định nghĩa các data class mà be và fe sẽ sử dụng để giao tiếp với nhau
// DTO dùng để trả dữ liệu về (Response)
@Serializable
data class ShortCourseDto(
    val id: Long,
    val thumbnail_url: String?,
    val type: String?,
    val is_favorite: Boolean? = null,
    val title: String,
    val creator_name: String,
    val studying_user_count: Int,
    val favorite_user_count: Int,
    val completed: Int? = null
)

@Serializable
data class CreatorDto(
    val name: String,
    val image: String?
)

@Serializable
data class SpeakingCourseDetailDto(
    val id: Long,
    val thumbnail_url: String?,
    val creator: CreatorDto,
    val type: String?,
    val is_favorite: Boolean? ,
    val title: String,
    val studying_user_count: Int,
    val favorite_user_count: Int,
    val description: String?,
    val list_speaking_day: List<ShortSpeakingDayDto>
)



// DTO dùng để nhận dữ liệu từ Client (Request Body cho luồng POST)
@Serializable
data class CreateCourseRequest(
    val topicId: Int? = null,
    val title: String,
    val description: String? = null,
    val creatorId: Int,
    val privacy: String
)