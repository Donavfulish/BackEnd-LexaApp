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
    val imageUrl: String? = null,
    val meaning: String,
    val example: String? = null,
    val partOfSpeech: String
)

@Serializable
data class CreateFlashcardRequest(
    val word: String,
    val transcription: String,
    val typeId: Int,
    val deckId: Int,
    val imageUrl: String? = null,
    val meaning: String,
    val example: String? = null,
    val partOfSpeechId: Int
)

@Serializable
data class UpdateFlashcardRequest(
    val id: Long,
    val word: String,
    val transcription: String,
    val typeId: Int,
    val imageUrl: String? = null,
    val meaning: String,
    val example: String? = null,
    val partOfSpeechId: Int
)