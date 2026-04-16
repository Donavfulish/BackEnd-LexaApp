package api.repository

import api.models.dto.AllCoursePaginationResponse
import api.models.dto.AllFlashcardPaginationResponse
import api.models.dto.AllFlashcardResultPaginationResponse
import api.models.dto.CreateFlashcardRequest
import api.models.dto.DetailFlashcard
import api.models.dto.DetailFlashcardWithResult
import api.models.dto.SearchInfo
import api.models.dto.UpdateFlashcardRequest
import api.models.dto.UpdateFlashcardResultRequest
import api.models.enum.OrderBy
import api.models.enum.ProgressStatus
import api.models.enum.SortBy
import api.models.tables.CoursesTable
import api.models.tables.FlashcardDecksTable
import api.models.tables.FlashcardResultsTable
import api.models.tables.FlashcardsTable
import api.models.tables.PartOfSpeechesTable
import api.models.tables.SearchEngine
import api.services.CloudinaryService
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import kotlin.collections.get
import kotlin.text.toInt
import kotlin.text.toLong

class FlashcardRepository {
    private suspend fun executeFlashcardPagination(
        deckId: Long,
        searchInfo: SearchInfo,
        nextCursor: Long?,
        baseQuery: Query,
        isResult: Boolean = false,
    ): Any{
        val query = searchInfo.query ?: ""
        val sortBy = SortBy.fromString(searchInfo.sortBy)
        val orderBy = OrderBy.fromString(searchInfo.order)
        val limit = searchInfo.limit ?: 10
        val lastId = nextCursor


        var isRevelant = false
        if (searchInfo.sortBy.isNullOrEmpty() and !searchInfo.query.isNullOrEmpty()) {
            isRevelant = true
        }

        var queryList = if (query.isNotEmpty()) {
            SearchEngine.searchAllFlashcard(query, deckId)
        } else null

        if (queryList != null && queryList.isEmpty()) {
            AllFlashcardPaginationResponse(emptyList(), searchInfo, null, 0)
        }

        val sortColumn = when (sortBy) {
            SortBy.CREATED -> FlashcardsTable.createdAt
            SortBy.TITLE -> FlashcardsTable.word
        }

        val lastValue = if (lastId != null && queryList.isNullOrEmpty()) {
            FlashcardsTable.slice(sortColumn).select { FlashcardsTable.id eq lastId }.singleOrNull()?.get(sortColumn)?.toString()
        } else null

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
            baseQuery.andWhere { FlashcardsTable.id inList queryList!!.map { it.id } }
        }

        if (lastId != null && lastValue != null && !isRevelant) {
            baseQuery.andWhere {
                val lastTime = if (sortBy == SortBy.CREATED) LocalDateTime.parse(lastValue) else null
                if (sortOrder == SortOrder.DESC) {
                    when (sortBy) {
                        SortBy.CREATED -> (FlashcardsTable.createdAt less lastTime!!) or ((FlashcardsTable.createdAt eq lastTime) and (FlashcardsTable.id less lastId))
                        SortBy.TITLE -> (FlashcardsTable.word less lastValue) or ((FlashcardsTable.word eq lastValue) and (FlashcardsTable.id less lastId))
                    }
                } else {
                    when (sortBy) {
                        SortBy.CREATED -> (FlashcardsTable.createdAt greater lastTime!!) or ((FlashcardsTable.createdAt eq lastTime) and (FlashcardsTable.id greater lastId))
                        SortBy.TITLE -> (FlashcardsTable.word greater lastValue) or ((FlashcardsTable.word eq lastValue) and (FlashcardsTable.id greater lastId))
                    }
                }
            }
        }

        if (!isRevelant)
            baseQuery.orderBy(sortColumn to sortOrder, FlashcardsTable.id to sortOrder)
                .limit(limit)

        val results = baseQuery.limit(limit).map{ row ->
            if(isResult){
                    val status = row[FlashcardResultsTable.status]
                    val finalStatus = when {
                        status == null -> ProgressStatus.FORGOTTEN
                        status == ProgressStatus.FORGOTTEN -> ProgressStatus.FORGOTTEN
                        else -> status
                    }

                    DetailFlashcardWithResult(
                        flashCard = DetailFlashcard(
                            id = row[FlashcardsTable.id].value.toInt(),
                            word = row[FlashcardsTable.word],
                            transcription = row[FlashcardsTable.transcription] ?: "",
                            type = row[FlashcardsTable.type]?.name ?: "",
                            deckId = row[FlashcardsTable.deckId].value.toInt(),
                            imageUrl = row[FlashcardsTable.imageUrl],
                            audioUrl = row[FlashcardsTable.audioUrl],
                            meaning = row[FlashcardsTable.meaningVi] ?: "",
                            example = row[FlashcardsTable.example],
                            partOfSpeech = row[PartOfSpeechesTable.name] ?: "",
                        ),
                        result = finalStatus
                    )
            } else{
                DetailFlashcard(
                    id = row[FlashcardsTable.id].value.toInt(),
                    word = row[FlashcardsTable.word],
                    transcription = row[FlashcardsTable.transcription] ?: "",
                    type = row[FlashcardsTable.type]?.name ?: "",
                    deckId = row[FlashcardsTable.deckId].value.toInt(),
                    imageUrl = row[FlashcardsTable.imageUrl],
                    audioUrl = row[FlashcardsTable.audioUrl],
                    meaning = row[FlashcardsTable.meaningVi] ?: "",
                    example = row[FlashcardsTable.example],
                    partOfSpeech = row[PartOfSpeechesTable.name] ?: "")
            }
        }
        val finalResults = if (isRevelant && queryList != null) {
            val idOrder = queryList!!.map { it.id }.withIndex().associate { it.value to it.index }
            results.sortedBy { item ->
                val id = if(item is DetailFlashcard) item.id else (item as DetailFlashcardWithResult).flashCard.id
                idOrder[id.toLong()] }
        } else results

        val lastItem = finalResults.lastOrNull()
        val nextCursorRes = if (finalResults.size == limit && lastItem != null) {
            if(lastItem is DetailFlashcard){
                lastItem.id
            } else {
                (lastItem as DetailFlashcardWithResult).flashCard.id
            }
        }
        else null

        return if(isResult)
        {
            AllFlashcardResultPaginationResponse(finalResults as List<DetailFlashcardWithResult>, searchInfo, nextCursorRes?.toLong(), totalCount)
        } else
        {
            AllFlashcardPaginationResponse(finalResults as List<DetailFlashcard>, searchInfo, nextCursorRes?.toLong(), totalCount)
        }

    }
    suspend fun getAllFlashcard(deckId: Long, searchInfo: SearchInfo, nextCursor: Long?): AllFlashcardPaginationResponse = dbQuery {
        val baseQuery = (FlashcardsTable innerJoin PartOfSpeechesTable)
            .select { FlashcardsTable.deckId eq deckId }
        executeFlashcardPagination(
            deckId,
            searchInfo,
            nextCursor,
            baseQuery
            ) as AllFlashcardPaginationResponse
    }
    suspend fun getAllFlashcardWithResult(deckId: Long, userId: Int, searchInfo: SearchInfo, nextCursor: Long?): AllFlashcardResultPaginationResponse = dbQuery {
        val baseQuery = (FlashcardsTable
            .innerJoin (PartOfSpeechesTable)
            .leftJoin(
                FlashcardResultsTable,
                onColumn = { FlashcardsTable.id },
                otherColumn = { FlashcardResultsTable.flashcardId },
                additionalConstraint = { FlashcardResultsTable.userId eq userId }
            )
        )
            .select {
                FlashcardsTable.deckId eq deckId
            }
        executeFlashcardPagination(
            deckId,
            searchInfo,
            nextCursor,
            baseQuery,
            true
        ) as AllFlashcardResultPaginationResponse
    }

    suspend fun updateFlashcard(userId: Int, request: UpdateFlashcardRequest) : Boolean = dbQuery {
        val oldFlashcard = (FlashcardsTable innerJoin FlashcardDecksTable)
            .slice(FlashcardsTable.imageUrl)
            .select { (FlashcardsTable.id eq request.flashcardId) and (FlashcardDecksTable.creatorId eq userId) }
            .singleOrNull()

        if (oldFlashcard == null) return@dbQuery false

        val oldImageUrl = oldFlashcard[FlashcardsTable.imageUrl]

        val updatedRows = FlashcardsTable.update({ FlashcardsTable.id eq request.flashcardId }) { statement ->
            request.word?.let { statement[word] = it }
            request.transcription?.let { statement[transcription] = it }
            request.meaning?.let { statement[meaningVi] = it }
            request.imageUrl?.let { statement[imageUrl] = it } // URL mới từ request
            request.example?.let { statement[example] = it }
            request.partOfSpeechId?.let { statement[partOfSpeechId] = it }
            request.typeId?.let { statement[type] = api.models.enum.VocabType.entries[it] }
            statement[updatedAt] = java.time.LocalDateTime.now()
        }

        val isSuccess = updatedRows > 0

        if (isSuccess && request.imageUrl != null && oldImageUrl != null) {
            CloudinaryService.deleteImage(oldImageUrl)
        }

        isSuccess
    }

    suspend fun deleteFlashcard(userId: Int, flashcardId: Long) : Boolean = dbQuery {
        val flashcardData = (FlashcardsTable innerJoin FlashcardDecksTable)
            .slice(FlashcardsTable.imageUrl)
            .select {
                (FlashcardsTable.id eq flashcardId) and (FlashcardDecksTable.creatorId eq userId)
            }
            .singleOrNull()

        if (flashcardData != null) {
            val urlToDelete = flashcardData[FlashcardsTable.imageUrl]

            FlashcardResultsTable.deleteWhere { FlashcardResultsTable.flashcardId eq flashcardId }

            val deletedRows = FlashcardsTable.deleteWhere { FlashcardsTable.id eq flashcardId }
            val isSuccess = deletedRows > 0

            if (isSuccess && !urlToDelete.isNullOrBlank()) {
                CloudinaryService.deleteImage(urlToDelete)
            }

            isSuccess
        } else {
            false
        }
    }

    suspend fun createFlashcard(userId: Int, request: CreateFlashcardRequest) : Long = dbQuery {
        val checkOwner = FlashcardDecksTable.select { (FlashcardDecksTable.id eq request.deckId) and (FlashcardDecksTable.creatorId eq userId) }
            .any()

        if(checkOwner){
            FlashcardsTable.insertAndGetId {
                it[deckId] = request.deckId.toLong()
                it[word] = request.word
                it[transcription] = request.transcription
                it[meaningVi] = request.meaning
                it[imageUrl] = request.imageUrl
                it[audioUrl] = request.audioUrl
                it[example] = request.example
                it[partOfSpeechId] = request.partOfSpeechId
                it[type] = api.models.enum.VocabType.entries[request.typeId]
            }.value
        } else {
            -1
        }
    }

    suspend fun updateFlashcardResults(userId: Int, request: UpdateFlashcardResultRequest): Boolean = dbQuery {
        try {
            request.results.forEach { item ->
                val exists = FlashcardResultsTable.select {
                    (FlashcardResultsTable.userId eq userId) and (FlashcardResultsTable.flashcardId eq item.flashcardId)
                }.any()

                if (exists) {
                    FlashcardResultsTable.update({
                        (FlashcardResultsTable.userId eq userId) and (FlashcardResultsTable.flashcardId eq item.flashcardId)
                    }) {
                        it[status] = item.status
                    }
                } else {
                    FlashcardResultsTable.insert {
                        it[this.userId] = userId
                        it[this.flashcardId] = item.flashcardId
                        it[this.status] = item.status
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}