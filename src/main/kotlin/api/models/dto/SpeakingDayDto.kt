package api.models.dto



import kotlinx.serialization.Serializable

// Định nghĩa các data class mà be và fe sẽ sử dụng để giao tiếp với nhau
// DTO dùng để trả dữ liệu về (Response)
@Serializable
data class ShortSpeakingDayDto(
    val speakingDayId: Long,
    val title: String,
    val completed: Int,
    val paragraphNum: Int
)

@Serializable
data class ShortParagraphSpeakingDayDto(
    val title: String?,
    val list_paragraphs: List<ShortParagraphDto>
)

@Serializable
data class CreateSpeakingDayRequest(
    val courseId: Long,
    val title: String?,
)
@Serializable
data class EditSpeakingDayRequest(
    val title: String?,
)

@Serializable
data class ParagraphOrderDto(
    val id: Long,
    val order: Long
)

@Serializable
data class ReorderParagraphsRequest(
    val paragraphs: List<ParagraphOrderDto>
)