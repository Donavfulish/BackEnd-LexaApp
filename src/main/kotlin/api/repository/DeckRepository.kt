package api.repository

import api.models.dto.CreateDeckRequest
import api.models.dto.DeckDto
import api.models.dto.DeckResultsTable
import api.models.dto.UpdateDeckRequest
import api.models.tables.FlashcardDecksTable
import api.models.tables.FlashcardsTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Duration
import java.time.LocalDateTime

class DeckRepository {

    suspend fun getVocabNumber(deckId: Long): Int = dbQuery {
        FlashcardsTable
            .select { FlashcardsTable.deckId eq deckId }
            .count()
            .toInt()
    }

//    suspend fun addDeckResult(deckId: Long): Long = dbQuery {
//
//    }

    suspend fun createDeck(request: CreateDeckRequest): Long = dbQuery {
        FlashcardDecksTable.insertAndGetId {
            it[title] = request.title
            it[creatorId] = request.creatorId
            it[privacy] = api.models.enum.PrivacyType.PRIVATE
        }.value
    }

    suspend fun updateDeck(request: UpdateDeckRequest): Boolean = dbQuery {
        FlashcardDecksTable.update({ FlashcardDecksTable.id eq request.id }) {
            request.title?.let { t -> it[title] = t }
            request.privacy?.let { p ->
                it[privacy] = api.models.enum.PrivacyType.valueOf(p.uppercase()) 
            }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteDeck(deckId: Long): Boolean = dbQuery {
        FlashcardDecksTable.deleteWhere { FlashcardDecksTable.id eq deckId } > 0
    }


    suspend fun getAllDecks(userId: Int?): List<DeckDto> = dbQuery {
        val vocabCountExpr = wrapAsExpression<Long>(
            FlashcardsTable
                .slice(FlashcardsTable.id.count())
                .select { FlashcardsTable.deckId eq FlashcardDecksTable.id }
        )
        val query = FlashcardDecksTable
            .slice(
                FlashcardDecksTable.id,
                FlashcardDecksTable.title,
                FlashcardDecksTable.createdAt,
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
