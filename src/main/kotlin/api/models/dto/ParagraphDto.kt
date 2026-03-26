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

// Request cho API Delete (PATCH /api/paragraph/{paragraphId})
@Serializable
data class DeleteParagraphRequest(
    val speakingDayId: Long,
    val paragraphOrder: Long? = null,
)