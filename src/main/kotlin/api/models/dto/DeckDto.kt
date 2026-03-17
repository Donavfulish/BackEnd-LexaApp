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
    val vocabNumber: Int,
    val createdAt: String
)

//@Serializable
//data class DetailDeck(
//    val id: Long,
//    val title: String,
//    val vocabNumber: Int,
//    val description: String?,
//    val privacy: String?,
//    val studentNumber: Int,
//    val favoriteNumber: Int,
//    val creator: String,
//    val topic: String?
//)

@Serializable
data class CreateDeckRequest(
    val title: String,
    val creatorId: Int,
)

@Serializable
data class UpdateDeckRequest(
    val id: Long,
    val title: String,
    val privacy: String? = null
)

@Serializable
data class InsertDeckResultRequest(
    val deckId: Long,
    val userId: Int,
    val rememberedCount: Int,
    val forgottenCount: Int
)