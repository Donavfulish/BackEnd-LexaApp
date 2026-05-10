package api.models.dto

import api.models.tables.FlashcardDecksTable
import api.models.tables.UsersTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

@Serializable
data class DeckDto(
    val id: Long,
    val title: String,
    val topic: TopicDto? = null,
    val vocabNumber: Int,
    val createdAt: String,
    val canDelete: Boolean
)

@Serializable
data class CreateDeckRequest(
    val title: String,
)

@Serializable
data class UpdateDeckRequest(
    val deckId: Long,
    val title: String? = null,
    val topicName: String? = null,
    val privacy: String? = null
)

@Serializable
data class DeckResult(
    val deckId: Long,
    val userId: Int,
    val rememberedCount: Int?,
    val forgottenCount: Int?
)

@Serializable
data class CreateDeckResultRequest(
    val deckId: Long,
    val userId: Int,
    val rememberedCount: Int,
    val forgottenCount: Int
)

@Serializable
data class UpdateDeckResultRequest(
    val deckId: Long,
    val rememberedCount: Int,
    val forgottenCount: Int
)

@Serializable
data class AllDeckPaginationResponse(
    val data: List<DeckDto>,
    val searchInfo: SearchInfo,
    val nextCursor: Long?= null,
    val totalItem: Long)
@Serializable
data class CopyDeckRequest(
    val deckId: Long,
)
