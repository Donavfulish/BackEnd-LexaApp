package api.models.dto

import kotlinx.serialization.Serializable

// Định nghĩa các data class mà be và fe sẽ sử dụng để giao tiếp với nhau
// DTO dùng để trả dữ liệu về (Response)
@Serializable
data class CourseDto(
    val id: Long,
    val topicId: Int?,
    val title: String,
    val description: String?,
    val creatorId: Int,
    val privacy: String?,
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