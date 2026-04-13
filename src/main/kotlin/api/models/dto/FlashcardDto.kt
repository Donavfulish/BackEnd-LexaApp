package api.models.dto

import api.models.enum.ProgressStatus
import io.ktor.http.Url
import kotlinx.serialization.Serializable

@Serializable
data class DetailFlashcard(
    val id: Int,
    val word: String,
    val transcription: String,
    val type: String,
    val deckId: Int,
    val imageUrl: String?,
    val audioUrl: String?,
    val meaning: String,
    val example: String?,
    val partOfSpeech: String
)

@Serializable
data class AllFlashcardPaginationResponse(
    val data: List<DetailFlashcard>,
    val searchInfo: SearchInfo,
    val nextCursor: Long?= null,
    val totalItem: Long)


@Serializable
data class DetailFlashcardWithResult (
    val flashCard: DetailFlashcard,
    val result: ProgressStatus,
)

@Serializable
data class CreateFlashcardRequest(
    val word: String,
    val transcription: String,
    val typeId: Int,
    val deckId: Long,
    var imageUrl: String? = null,
    val audioUrl: String? = null,
    val meaning: String,
    val example: String? = null,
    val partOfSpeechId: Int
)

@Serializable
data class UpdateFlashcardRequest(
    var flashcardId: Long,
    val word: String? = null,
    val transcription: String? = null,
    val typeId: Int? = null,
    var imageUrl: String? = null,
    val audioUrl: String? = null,
    val meaning: String? = null,
    val example: String? = null,
    val partOfSpeechId: Int? = null
)


@Serializable
data class FlashcardResultItem(
    val flashcardId: Long,
    val status: ProgressStatus? = null
)

@Serializable
data class UpdateFlashcardResultRequest(
    val deckId: Long,
    val results: List<FlashcardResultItem>
)