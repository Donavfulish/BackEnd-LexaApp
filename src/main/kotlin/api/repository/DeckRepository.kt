package api.repository

import api.models.dto.CreateDeckRequest
import api.models.dto.DeckDto
import api.models.dto.DeckResult
import api.models.dto.ShortCourseDto
import api.models.dto.TopicDto
import api.models.dto.UpdateDeckRequest
import api.models.enum.PrivacyType
import api.models.tables.CoursesTable
import api.models.tables.CoursesTable.deckId
import api.models.tables.DeckResultsTable
import api.models.tables.FlashcardDecksTable
import api.models.tables.FlashcardsTable
import api.models.tables.SpeakingDaysTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import api.models.tables.TopicsTable
import api.models.tables.UserFavoriteCoursesTable
import api.models.tables.UserFavoriteDecksTable
import api.models.tables.UsersTable
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
            it[privacy] = api.models.enum.PrivacyType.PRIVATE
        }.value
    }

    suspend fun updateDeck(request: UpdateDeckRequest): Boolean = dbQuery {
        FlashcardDecksTable.update({ FlashcardDecksTable.id eq request.deckId }) {
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
    suspend fun getFavoriteDecks(userId: Int): List<ShortCourseDto> = dbQuery {

        UserFavoriteDecksTable
        UserFavoriteDecksTable
            .innerJoin(CoursesTable, { deckId }, { deckId })
            .innerJoin(UsersTable, { CoursesTable.creatorId }, { id })
            .leftJoin(TopicsTable, { CoursesTable.topicId }, { id })
            .select {
                (CoursesTable.privacy eq PrivacyType.PUBLIC) and
                        (UserFavoriteDecksTable.userId eq userId)
            }
            .map { row ->
                val courseId = row[CoursesTable.id]
                val isFavorite = true
                val studyingUserCount: Int = run {
                    val learnerCountExpr = SpeakingParagraphResultsTable.userId.countDistinct()
                    (SpeakingParagraphResultsTable
                        .innerJoin(SpeakingParagraphsTable)
                        .innerJoin(SpeakingDaysTable)
                        .slice(learnerCountExpr)
                        .select {
                            SpeakingDaysTable.courseId eq courseId
                        }
                        .firstOrNull()
                        ?.get(learnerCountExpr) ?: 0L
                            ).toInt()
                }
                val vocabNumber = row[CoursesTable.deckId]?.let { dId ->
                    FlashcardsTable.select { FlashcardsTable.deckId eq dId }.count()
                } ?: 0L
                val favoriteCountExpr = (UserFavoriteCoursesTable.userId.count())
                val favoriteUserCount = (UserFavoriteCoursesTable
                    .slice(favoriteCountExpr)
                    .select { UserFavoriteCoursesTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(favoriteCountExpr) ?: 0L).toInt()

                ShortCourseDto(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    topic = TopicDto(
                        id = row[TopicsTable.id]?.value ?: 0,
                        name = row[TopicsTable.name] ?: "",
                        colorHex = row[TopicsTable.color] ?: "#636AE8"),
                    title = row[CoursesTable.title],
                    description = row[CoursesTable.description] ?: "",
                    creator_name = row[UsersTable.name],
                    creator_avatar_url = row[UsersTable.avatarUrl] ?: "",
                    is_favorite = isFavorite,
                    vocabNumber = vocabNumber.toInt(),
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount
                )
            }
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
    suspend fun addFavoriteDeck(userId: Int, deckId: Long): Boolean = dbQuery {
        val isAlreadyFavorited = UserFavoriteDecksTable
            .select {
                (UserFavoriteDecksTable.userId eq userId) and
                        (UserFavoriteDecksTable.deckId eq deckId)
            }
            .singleOrNull() != null

        if (!isAlreadyFavorited) {
            UserFavoriteDecksTable.insert {
                it[this.userId] = userId
                it[this.deckId] = deckId
            }
        }
        true
    }
    suspend fun removeFavoriteDeck(userId: Int, deckId: Long): Boolean = dbQuery {
        val deletedRows = UserFavoriteDecksTable.deleteWhere {
            (UserFavoriteDecksTable.userId eq userId) and
                    (UserFavoriteDecksTable.deckId eq deckId)
        }
        true
    }
}
