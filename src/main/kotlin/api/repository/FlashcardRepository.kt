package api.repository

import api.models.dto.CreateFlashcardRequest
import api.models.dto.DetailFlashcard
import api.models.dto.UpdateFlashcardRequest
import api.models.tables.FlashcardsTable
import api.models.tables.PartOfSpeechesTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
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



    suspend fun updateFlashcard(request: UpdateFlashcardRequest) : Boolean = dbQuery {
        FlashcardsTable.update({FlashcardsTable.id eq request.id}){
            it[word] = request.word
            it[transcription] = request.transcription
            it[meaningVi] = request.meaning
            it[imageUrl] = request.imageUrl
            it[example] = request.example
            it[partOfSpeechId] = request.partOfSpeechId

            it[type] = api.models.enum.VocabType.entries[request.typeId]
            it[updatedAt] = java.time.LocalDateTime.now()
        } > 0
    }

    suspend fun deleteFlashcard(flashcardId: Long) : Boolean = dbQuery {
        FlashcardsTable.deleteWhere { FlashcardsTable.id eq flashcardId} > 0
    }

    suspend fun createFlashcard(request: CreateFlashcardRequest) : Long = dbQuery {
        FlashcardsTable.insertAndGetId {
            it[deckId] = request.deckId.toLong()
            it[word] = request.word
            it[transcription] = request.transcription
            it[meaningVi] = request.meaning
            it[imageUrl] = request.imageUrl
            it[example] = request.example
            it[partOfSpeechId] = request.partOfSpeechId
            it[type] = api.models.enum.VocabType.entries[request.typeId]
        }.value
    }

}