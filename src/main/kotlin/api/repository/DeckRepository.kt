package api.repository

import api.models.dto.AllCoursePaginationResponse
import api.models.dto.AllDeckPaginationResponse
import api.models.dto.CopyDeckRequest
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import api.models.dto.CreateDeckRequest
import api.models.dto.CreateDeckResultRequest
import api.models.dto.DeckDto
import api.models.dto.DeckResult
import api.models.dto.SearchInfo
import api.models.dto.ShortCourseDto
import api.models.dto.TopicDto
import api.models.dto.UpdateDeckRequest
import api.models.dto.UpdateDeckResultRequest
import api.models.enum.OrderBy
import api.models.enum.PrivacyType
import api.models.enum.SortBy
import api.models.tables.CoursesTable
import api.models.tables.CoursesTable.deckId
import api.models.tables.DeckResultsTable
import api.models.tables.FlashcardDecksTable
import api.models.tables.FlashcardResultsTable
import api.models.tables.FlashcardsTable
import api.models.tables.SearchEngine
import api.models.tables.SpeakingDaysTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import api.models.tables.TopicsTable
import api.models.tables.UserFavoriteDecksTable
import api.models.tables.UserFavoriteCoursesTable
import api.models.tables.UsersTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Duration
import java.time.LocalDateTime

class DeckRepository {

    suspend fun getDeckSuggestions(query: String): List<String> {
        return SearchEngine.searchGlobalDecks(query).take(10).map { it.title }
    }

    suspend fun createDeck(userId: Int, request: CreateDeckRequest): Long = dbQuery {
        FlashcardDecksTable.insertAndGetId {
            it[title] = request.title
            it[creatorId] = userId
            it[privacy] = api.models.enum.PrivacyType.PUBLIC
        }.value
    }

    suspend fun updateDeck(userId: Int, request: UpdateDeckRequest): Boolean = dbQuery {
        val targetTopicId = request.topicName?.let { name ->
            TopicsTable.slice(TopicsTable.id)
                .select { TopicsTable.name eq name }
                .singleOrNull()?.get(TopicsTable.id)
        }

        FlashcardDecksTable.update({
            (FlashcardDecksTable.id eq request.deckId) and (FlashcardDecksTable.creatorId eq userId)}) {
            request.title?.let { t -> it[title] = t }
            targetTopicId?.let { id -> it[topicId] = id }
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

    suspend fun updateDeckResult(request: UpdateDeckResultRequest, userId: Int): Boolean = dbQuery {
        DeckResultsTable.update({
            (DeckResultsTable.deckId eq request.deckId) and (DeckResultsTable.userId eq userId)}){
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
                    favorite_user_count = favoriteUserCount,
                    created_at = row[CoursesTable.createdAt].toString()
                )
            }
    }
    
    suspend fun getAllDecks(userId: Int, searchInfo: SearchInfo, nextCursor: Long?): AllDeckPaginationResponse = dbQuery {
        val query = searchInfo.query ?: ""
        val sortBy = SortBy.fromString(searchInfo.sortBy)
        val orderBy = OrderBy.fromString(searchInfo.order)
        val limit = searchInfo.limit ?: 20
        val lastId = nextCursor

        var isRevelant = false
        if (searchInfo.sortBy.isNullOrEmpty() and !searchInfo.query.isNullOrEmpty()) {
            isRevelant = true
        }

        var queryList = if (query.isNotEmpty()) {
            SearchEngine.searchAllDecks(query, userId)
        } else null

        if (queryList != null && queryList.isEmpty()) {
            AllDeckPaginationResponse(emptyList(), searchInfo, null, 0)
        }

        val sortColumn = when (sortBy) {
            SortBy.CREATED -> FlashcardDecksTable.createdAt
            SortBy.TITLE -> FlashcardDecksTable.title
        }

        val lastValue = if (lastId != null && queryList.isNullOrEmpty()) {
            FlashcardDecksTable.slice(sortColumn).select { FlashcardDecksTable.id eq lastId }.singleOrNull()?.get(sortColumn)?.toString()
        } else null
        val vocabCountExpr = wrapAsExpression<Long>(
            FlashcardsTable
                .slice(FlashcardsTable.id.count())
                .select { FlashcardsTable.deckId eq FlashcardDecksTable.id }
        )

        val courseLinkCountExpr = wrapAsExpression<Long>(
            CoursesTable
                .slice(CoursesTable.id.count())
                .select { CoursesTable.deckId eq FlashcardDecksTable.id }
        )

        val baseQuery = (FlashcardDecksTable leftJoin TopicsTable)
            .slice(
                FlashcardDecksTable.id,
                FlashcardDecksTable.title,
                FlashcardDecksTable.createdAt,
                TopicsTable.id,
                TopicsTable.name,
                TopicsTable.color,
                vocabCountExpr,
                courseLinkCountExpr
            )
            .select { FlashcardDecksTable.creatorId eq userId }

        var totalCount = baseQuery.count()
        val sortOrder = if (orderBy == OrderBy.ASC) SortOrder.ASC else SortOrder.DESC

        if (!queryList.isNullOrEmpty()) {
            totalCount = queryList!!.size.toLong()
            if (lastId != null) {
                val startIndex = queryList!!.indexOfFirst { it.id == lastId }
                queryList = queryList!!.subList(startIndex + 1, if (startIndex + 10 > queryList!!.size) queryList!!.size else startIndex + 11)
            } else {
                queryList = queryList!!.subList(0, if (10 > queryList!!.size) queryList!!.size else 10)
            }
            baseQuery.andWhere { FlashcardDecksTable.id inList queryList!!.map { it.id } }
        }

        if (lastId != null && lastValue != null && !isRevelant) {
            baseQuery.andWhere {
                val lastTime = if (sortBy == SortBy.CREATED) LocalDateTime.parse(lastValue) else null
                if (sortOrder == SortOrder.DESC) {
                    when (sortBy) {
                        SortBy.CREATED -> (FlashcardDecksTable.createdAt less lastTime!!) or ((FlashcardDecksTable.createdAt eq lastTime) and (FlashcardDecksTable.id less lastId))
                        SortBy.TITLE -> (FlashcardDecksTable.title less lastValue) or ((FlashcardDecksTable.title eq lastValue) and (FlashcardDecksTable.id less lastId))
                    }
                } else {
                    when (sortBy) {
                        SortBy.CREATED -> (FlashcardDecksTable.createdAt greater lastTime!!) or ((FlashcardDecksTable.createdAt eq lastTime) and (FlashcardDecksTable.id greater lastId))
                        SortBy.TITLE -> (FlashcardDecksTable.title greater lastValue) or ((FlashcardDecksTable.title eq lastValue) and (FlashcardDecksTable.id greater lastId))
                    }
                }
            }
        }

        if (!isRevelant) baseQuery.orderBy(sortColumn, sortOrder).limit(limit)

        val results = baseQuery.map { row ->
            DeckDto(
                id = row[FlashcardDecksTable.id].value,
                title = row[FlashcardDecksTable.title],
                topic = row.getOrNull(TopicsTable.id)?.let { topicId ->
                    TopicDto(
                        id = topicId.value,
                        name = row.getOrNull(TopicsTable.name) ?: "",
                        colorHex = row.getOrNull(TopicsTable.color) ?: "#FFFFFF",
                    )
                },
                vocabNumber = row[vocabCountExpr]?.toInt() ?: 0,
                createdAt = convertTime(row[FlashcardDecksTable.createdAt]) ,
                canDelete = (row[courseLinkCountExpr]?.toInt() ?: 0) == 0
            )
        }

        val finalResults = if (isRevelant && queryList != null) {
            val idOrder = queryList!!.map { it.id }.withIndex().associate { it.value to it.index }
            results.sortedBy { idOrder[it.id] }
        } else results

        val lastItem = finalResults.lastOrNull()
        val nextCursorRes = if (finalResults.size == limit && lastItem != null) lastItem.id else null

        AllDeckPaginationResponse(finalResults, searchInfo, nextCursorRes, totalCount)
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


    suspend fun copyDeck(userId: Int, request: CopyDeckRequest): Boolean = dbQuery {
        val originalDeckId: Long = request.deckId;
        val originalDeck = FlashcardDecksTable
            .select { FlashcardDecksTable.id eq originalDeckId }
            .singleOrNull() ?: return@dbQuery false


        val courseTopicId = (CoursesTable leftJoin FlashcardDecksTable)
            .slice(CoursesTable.topicId)
            .select { FlashcardDecksTable.id eq request.deckId }
            .singleOrNull()
            ?.get(CoursesTable.topicId)


        val newDeckId = FlashcardDecksTable.insertAndGetId {
            it[creatorId] = userId
            it[title] = originalDeck[title]
            it[description] = originalDeck[description]
            it[topicId] = courseTopicId ?: throw IllegalArgumentException("Topic ID cannot be null")
            it[privacy] = api.models.enum.PrivacyType.PUBLIC
        }.value

        val originalFlashcards = FlashcardsTable
            .select { FlashcardsTable.deckId eq originalDeckId }
            .toList()

        if (originalFlashcards.isNotEmpty()) {
            FlashcardsTable.batchInsert(originalFlashcards) { originalCard ->
                this[FlashcardsTable.deckId] = newDeckId
                this[FlashcardsTable.imageUrl] = originalCard[FlashcardsTable.imageUrl]
                this[FlashcardsTable.audioUrl] = originalCard[FlashcardsTable.audioUrl]
                this[FlashcardsTable.transcription] = originalCard[FlashcardsTable.transcription]
                this[FlashcardsTable.word] = originalCard[FlashcardsTable.word]
                this[FlashcardsTable.meaningVi] = originalCard[FlashcardsTable.meaningVi]
                this[FlashcardsTable.type] = originalCard[FlashcardsTable.type]
                this[FlashcardsTable.partOfSpeechId] = originalCard[FlashcardsTable.partOfSpeechId]
                this[FlashcardsTable.example] = originalCard[FlashcardsTable.example]

            }
        }
        true
    }
}
