package api.repository

import api.models.dto.CreateParagraphRequest
import api.models.dto.ParagraphResponseDto
import api.models.dto.UpdateParagraphRequest
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

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

    suspend fun getParagraphById(paragraphId: Long): ParagraphResponseDto? = dbQuery {
        SpeakingParagraphsTable.select { SpeakingParagraphsTable.id eq paragraphId }
            .map {
                ParagraphResponseDto(
                    id = it[SpeakingParagraphsTable.id].value,
                    paragraph = it[SpeakingParagraphsTable.paragraph],
                    audioUrl = it[SpeakingParagraphsTable.audioUrl],
                    paragraphOrder = it[SpeakingParagraphsTable.paragraphOrder]
                )
            }.singleOrNull()
    }

    suspend fun updateParagraphInfo(paragraphId: Long, request: UpdateParagraphRequest): Boolean = dbQuery {
        val updatedRows = SpeakingParagraphsTable.update({ SpeakingParagraphsTable.id eq paragraphId }) {
            // Chỉ update nếu client có truyền giá trị
            request.paragraph?.let { paragraphValue -> it[paragraph] = paragraphValue }
            request.audioUrl?.let { audioValue -> it[audioUrl] = audioValue }
        }
        updatedRows > 0
    }

    suspend fun deleteParagraph(paragraphId: Long): Boolean = dbQuery {
        // Xóa các record liên quan trong bảng Result trước để tránh lỗi Foreign Key Constraint
        SpeakingParagraphResultsTable.deleteWhere { SpeakingParagraphResultsTable.paragraphId eq paragraphId }

        // Sau đó xóa paragraph chính
        val deletedRows = SpeakingParagraphsTable.deleteWhere { SpeakingParagraphsTable.id eq paragraphId }
        deletedRows > 0
    }
}