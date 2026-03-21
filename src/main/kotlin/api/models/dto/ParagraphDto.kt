package api.models.dto
import kotlinx.serialization.Serializable

@Serializable
data class ShortParagraphDto(
    val id: Long,
    val paragraph: String?,
    val paragraph_order: Long?,
){

}