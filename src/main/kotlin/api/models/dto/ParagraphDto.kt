package api.models.dto
import kotlinx.serialization.Serializable

@Serializable
data class ShortParagraphDto(
    val id: Long,
    val paragraph: String?,
    val paragraph_order: Long?,
){

}

@Serializable
data class CreateParagraphRequest(
    val speakingDayId: Long,
    val paragraphOrder: Long? = null,
    val paragraph: String,
    val audioURL: String? = null
)



@Serializable
data class ParagraphResponseDto(
    val id: Long,
    val paragraph: String?,
    val audioUrl: String?,
    val paragraphOrder: Long? = null,
)

@Serializable
data class UpdateParagraphRequest(
    val paragraph: String? = null,
    val audioUrl: String? = null
)


// Model mô tả từng từ được đánh giá
@Serializable
data class WordEvaluationItem(
    val word: String,
    val score: Int,
    val status: String
)

// DTO nhận dữ liệu update kết quả paragraph từ Client
@Serializable
data class UpdateParagraphResultRequest(
    val paragraphId: Long,
    val wordEvaluation: List<WordEvaluationItem>? = null,
    val goodCount: Int? = null,
    val mediumCount: Int? = null,
    val badCount: Int? = null,
    val userAudioUrl: String? = null
)


// DTO trả về cho FE khi update kết quả thành công
@Serializable
data class ParagraphResultResponseDto(
    val userId: Int,
    val paragraphId: Long,
    val wordEvaluation: List<WordEvaluationItem>?, // Trả về dạng mảng JSON thay vì String cho FE dễ đọc
    val goodCount: Int?,
    val mediumCount: Int?,
    val badCount: Int?,
    val userAudioUrl: String?
)

@Serializable
data class SubmitBulkDailyResultRequest(
    val speakingDayId: Long,
    val results: List<UpdateParagraphResultRequest>
)