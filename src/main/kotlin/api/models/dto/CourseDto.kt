package api.models.dto

import kotlinx.serialization.Serializable

// Định nghĩa các data class mà be và fe sẽ sử dụng để giao tiếp với nhau
// DTO dùng để trả dữ liệu về (Response)
@Serializable
data class ShortCourseDto(
    val id: Long,
    val thumbnail_url: String?,
    val topic: TopicDto,
    val is_favorite: Boolean? = null,
    val title: String,
    val description: String,
    val creator_name: String,
    val creator_avatar_url: String,
    val vocabNumber: Int,
    val studying_user_count: Int,
    val favorite_user_count: Int,
    val completed: Int? = null
)
@Serializable
data class GetFeaturedCourseResponse(
    val id: Long,
    val thumbnail_url: String?,
    val topic: TopicDto,
    val is_favorite: Boolean? = null,
    val title: String,
    val creator_name: String,
    val creator_avatar_url: String,
    val studying_user_count: Int,
    val favorite_user_count: Int,
)
@Serializable
data class GetStudyingCourseResponse(
    val id: Long,
    val title: String,
    val topic: TopicDto,
    val progress: Int,
    val thumbnail_url: String?
)

@Serializable
data class CreatorDto(
    val id: Int,
    val name: String,
    val image: String?
)

@Serializable
data class SpeakingCourseDetailDto(
    val id: Long,
    val thumbnail_url: String?,
    val creator: CreatorDto,
    val type: String?,
    val typeColor: String?,
    val is_favorite: Boolean? ,
    val title: String,
    val studying_user_count: Int,
    val favorite_user_count: Int,
    val description: String?,
    val deckId: Long,
    val list_speaking_day: List<ShortSpeakingDayDto>,
    val list_topic: List<TopicDto>
)



// DTO dùng để nhận dữ liệu từ Client (Request Body cho luồng POST)
@Serializable
data class CreateCourseRequest(
    val topicId: Int? = null,
    val title: String,
    val description: String? = null,
    val privacy: String,
    val deckId: Long?,
    val thumbnailUrl: String? = null
)
@Serializable
data class EditCourseRequest(
    val topicId: Int? = null,
    val title: String,
    val description: String? = null,
    val privacy: String,
    val thumbnailUrl: String? = null
)