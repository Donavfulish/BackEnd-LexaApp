package api.models.dto

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
data class CreateFlashcardRequest(
    val word: String,
    val transcription: String,
    val typeId: Int,
    val deckId: Long,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val meaning: String,
    val example: String? = null,
    val partOfSpeechId: Int
)

@Serializable
data class UpdateFlashcardRequest(
    val flashcardId: Long,
    val word: String? = null,
    val transcription: String? = null,
    val typeId: Int? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val meaning: String? = null,
    val example: String? = null,
    val partOfSpeechId: Int? = null
)