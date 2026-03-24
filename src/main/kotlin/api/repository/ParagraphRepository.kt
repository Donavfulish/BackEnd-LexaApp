package api.repository

import api.models.dto.CreateParagraphRequest
import api.models.dto.ParagraphResponseDto
import api.models.tables.SpeakingParagraphsTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.insertAndGetId

class ParagraphRepository {
    suspend fun createParagraph(request: CreateParagraphRequest): ParagraphResponseDto = dbQuery {
        val insertId = SpeakingParagraphsTable.insertAndGetId {
            it[speakingDayId] = request.speakingDayId
            it[paragraphOrder] = request.paragraphOrder
            it[paragraph] = request.paragraph
            it[audioUrl] = request.audioURL
        }

        ParagraphResponseDto(
            id = insertId.value,
            paragraph = request.paragraph,
            paragraphOrder = request.paragraphOrder,
            audioUrl = request.audioURL,
        )
    }
}