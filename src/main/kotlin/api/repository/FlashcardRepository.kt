package api.repository

import api.models.dto.CreateFlashcardRequest
import api.models.dto.DetailFlashcard
import api.models.dto.DetailFlashcardWithResult
import api.models.dto.UpdateFlashcardRequest
import api.models.dto.UpdateFlashcardResultRequest
import api.models.enum.ProgressStatus
import api.models.tables.FlashcardDecksTable
import api.models.tables.FlashcardResultsTable
import api.models.tables.FlashcardsTable
import api.models.tables.PartOfSpeechesTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

class FlashcardRepository {
    suspend fun getAllFlashcard(deckId: Long): List<DetailFlashcard> = dbQuery {
        (FlashcardsTable innerJoin PartOfSpeechesTable)
            .select { FlashcardsTable.deckId eq deckId }
            .map { row ->
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
                    partOfSpeech = row[PartOfSpeechesTable.name] ?: "",)
            }
    }

    suspend fun getAllFlashcardWithResult(deckId: Long, userId: Int): List<DetailFlashcardWithResult> = dbQuery {
        (FlashcardsTable
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
            .map { row ->

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
            }
    }

    suspend fun updateFlashcard(userId: Int, request: UpdateFlashcardRequest) : Boolean = dbQuery {
        val checkOwner = (FlashcardsTable innerJoin FlashcardDecksTable)
            .select {(FlashcardsTable.id eq request.flashcardId) and (FlashcardDecksTable.creatorId eq userId)}
            .any()

        if(checkOwner) {
            FlashcardsTable.update({ FlashcardsTable.id eq request.flashcardId }) { statement ->
                request.word?.let { statement[word] = it }
                request.transcription?.let { statement[transcription] = it }
                request.meaning?.let { statement[meaningVi] = it }
                request.imageUrl?.let { statement[imageUrl] = it }
                request.example?.let { statement[example] = it }
                request.partOfSpeechId?.let { statement[partOfSpeechId] = it }
                request.typeId?.let { statement[type] = api.models.enum.VocabType.entries[it] }
                statement[updatedAt] = java.time.LocalDateTime.now()
            } > 0
         } else {
            false
        }
    }

    suspend fun deleteFlashcard(userId: Int, flashcardId: Long) : Boolean = dbQuery {
        val checkOwner = (FlashcardsTable innerJoin FlashcardDecksTable)
            .select {
                (FlashcardsTable.id eq flashcardId) and
                        (FlashcardDecksTable.creatorId eq userId)
            }
            .any()
        if(checkOwner){
            FlashcardResultsTable.deleteWhere { FlashcardResultsTable.flashcardId eq flashcardId }
            FlashcardsTable.deleteWhere { FlashcardsTable.id eq flashcardId} > 0
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