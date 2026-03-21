package api.models.dto



import kotlinx.serialization.Serializable

// Định nghĩa các data class mà be và fe sẽ sử dụng để giao tiếp với nhau
// DTO dùng để trả dữ liệu về (Response)
@Serializable
data class ShortSpeakingDayDto(
    val title: String,
    val completed: Int
)

@Serializable
data class ShortParagraphSpeakingDayDto(
    val title: String,
    val list_paragraphs: List<ShortParagraphDto>
){

}