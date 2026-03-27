package api.repository

import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import api.models.dto.CreateDeckRequest
import api.models.dto.CreateDeckResultRequest
import api.models.dto.DeckDto
import api.models.dto.DeckResult
import api.models.dto.TopicDto
import api.models.dto.UpdateDeckRequest
import api.models.dto.UpdateDeckResultRequest
import api.models.tables.DeckResultsTable
import api.models.tables.FlashcardDecksTable
import api.models.tables.FlashcardResultsTable
import api.models.tables.FlashcardsTable
import api.models.tables.TopicsTable
import api.models.tables.UserFavoriteDecksTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Duration
import java.time.LocalDateTime

class DeckRepository {

    suspend fun createDeck(request: CreateDeckRequest): Long = dbQuery {
        FlashcardDecksTable.insertAndGetId {
            it[title] = request.title
            it[creatorId] = request.creatorId
            it[privacy] = api.models.enum.PrivacyType.PUBLIC
        }.value
    }

    suspend fun updateDeck(userId: Int, request: UpdateDeckRequest): Boolean = dbQuery {
        FlashcardDecksTable.update({
            (FlashcardDecksTable.id eq request.deckId) and (FlashcardDecksTable.creatorId eq userId)}) {
            request.title?.let { t -> it[title] = t }
            request.privacy?.let { p ->
                it[privacy] = api.models.enum.PrivacyType.valueOf(p.uppercase()) 
            }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteDeck(userId: Int, deckId: Long): Boolean = dbQuery {
        val checkOwner = FlashcardDecksTable.select { (FlashcardDecksTable.id eq deckId) and (FlashcardDecksTable.creatorId eq userId) }
            .any()
        if(checkOwner) {
            FlashcardResultsTable.deleteWhere {
                FlashcardResultsTable.flashcardId inSubQuery FlashcardsTable
                    .slice(FlashcardsTable.id)
                    .select { FlashcardsTable.deckId eq deckId }
            }
            FlashcardsTable.deleteWhere { FlashcardsTable.deckId eq deckId }
            DeckResultsTable.deleteWhere { DeckResultsTable.deckId eq deckId }
            UserFavoriteDecksTable.deleteWhere { UserFavoriteDecksTable.deckId eq deckId }
            FlashcardDecksTable.deleteWhere { FlashcardDecksTable.id eq deckId } > 0
        } else {
            false
        }
    }

    suspend fun createDeckResult(request: CreateDeckResultRequest): Boolean = dbQuery {
        DeckResultsTable.insert{
            it[userId] = request.userId
            it[deckId] = request.deckId
            it[rememberedCount] = request.rememberedCount
            it[forgottenCount] = request.forgottenCount
        }.insertedCount > 0
    }

    suspend fun updateDeckResult(request: UpdateDeckResultRequest): Boolean = dbQuery {
        DeckResultsTable.update({
            (DeckResultsTable.deckId eq request.deckId) and (DeckResultsTable.userId eq request.userId)}){
            it[rememberedCount] = request.rememberedCount
            it[forgottenCount] = request.forgottenCount
        } > 0
    }

    suspend fun getDeckResult(userId: Int, deckId: Long): DeckResult? = dbQuery {
        DeckResultsTable
            .select{ (DeckResultsTable.deckId eq deckId) and (DeckResultsTable.userId eq userId) }
            .map{ row ->
                DeckResult(
                    userId = row[DeckResultsTable.userId].value,
                    deckId = row[DeckResultsTable.deckId].value,
                    rememberedCount = row[DeckResultsTable.rememberedCount],
                    forgottenCount = row[DeckResultsTable.forgottenCount]
                )
            }
            .singleOrNull()
    }

    suspend fun getAllDecks(userId: Int?): List<DeckDto> = dbQuery {
        val vocabCountExpr = wrapAsExpression<Long>(
            FlashcardsTable
                .slice(FlashcardsTable.id.count())
                .select { FlashcardsTable.deckId eq FlashcardDecksTable.id }
        )
        val query = (FlashcardDecksTable leftJoin TopicsTable)
            .slice(
                FlashcardDecksTable.id,
                FlashcardDecksTable.title,
                FlashcardDecksTable.createdAt,
                TopicsTable.id,
                TopicsTable.name,
                TopicsTable.color,
                vocabCountExpr
            )
        val finalQuery = if (userId != null) {
            query.select { FlashcardDecksTable.creatorId eq userId }
        } else {
            query.selectAll()
        }
        finalQuery.map { row ->
            DeckDto(
                id = row[FlashcardDecksTable.id].value,
                title = row[FlashcardDecksTable.title],
                topic = TopicDto(
                    id = row[TopicsTable.id].value,
                    name = row[TopicsTable.name] ?: "",
                    colorHex = row[TopicsTable.color] ?: "#FFFFFF",
                ),
                vocabNumber = row[vocabCountExpr]?.toInt() ?: 0,
                createdAt = convertTime(row[FlashcardDecksTable.createdAt])
            )
        }
    }

    private fun convertTime(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val duration = Duration.between(dateTime, now)
        val seconds = duration.seconds

        return when {
            seconds < 60 -> "Vừa xong"
            seconds < 3600 -> "${seconds / 60} phút trước"
            seconds < 86400 -> "${seconds / 3600} giờ trước"
            seconds < 2592000 -> "${seconds / 86400} ngày trước"
            seconds < 31536000 -> "${seconds / 2592000} tháng trước"
            else -> "${seconds / 31536000} năm trước"
        }
    }
}
