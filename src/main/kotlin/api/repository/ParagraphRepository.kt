package api.repository

import api.models.dto.CreateParagraphRequest
import api.models.dto.ParagraphResponseDto
import api.models.dto.ParagraphResultResponseDto
import api.models.dto.UpdateParagraphRequest
import api.models.dto.UpdateParagraphResultRequest
import api.models.dto.WordEvaluationItem
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
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

    suspend fun upsertParagraphResult (
        userId: Int,
        request: UpdateParagraphResultRequest
    ) : ParagraphResultResponseDto = dbQuery {

        // chuyển mảng object -> chuỗi string
        val evaluationJsonString = request.wordEvaluation?.let { Json.encodeToString(it) }

        // Kiểm tra xem record đã tồn tại chưa
        val existingResult = SpeakingParagraphResultsTable.select {
            (SpeakingParagraphResultsTable.userId eq userId) and
                    (SpeakingParagraphResultsTable.paragraphId eq request.paragraphId)
        }.singleOrNull()

        if (existingResult != null) {
            // UPDATE nếu đã có
            SpeakingParagraphResultsTable.update({
                (SpeakingParagraphResultsTable.userId eq userId) and
                        (SpeakingParagraphResultsTable.paragraphId eq request.paragraphId)
            }) {
                // Chỉ update các trường có dữ liệu gửi lên
                request.goodCount?.let { count -> it[goodCount] = count }
                request.mediumCount?.let { count -> it[mediumCount] = count }
                request.badCount?.let { count -> it[badCount] = count }
                request.userAudioUrl?.let { url -> it[userAudioUrl] = url }
                evaluationJsonString?.let { json -> it[wordEvaluation] = json }
            }
        } else {
            // INSERT nếu là lần đầu nộp bài
            SpeakingParagraphResultsTable.insert {
                it[this.userId] = userId
                it[this.paragraphId] = request.paragraphId
                it[this.goodCount] = request.goodCount
                it[this.mediumCount] = request.mediumCount
                it[this.badCount] = request.badCount
                it[this.userAudioUrl] = request.userAudioUrl
                it[this.wordEvaluation] = evaluationJsonString
            }
        }

        // Truy vấn lại để lấy record mới nhất từ DB
        val updatedRow = SpeakingParagraphResultsTable.select {
            (SpeakingParagraphResultsTable.userId eq userId) and
                    (SpeakingParagraphResultsTable.paragraphId eq request.paragraphId)
        }.single()

        // Giải mã JSON String từ DB ra lại List để trả về cho FE
        val parsedEvaluation: List<WordEvaluationItem>? = updatedRow[SpeakingParagraphResultsTable.wordEvaluation]?.let {
            try { Json.decodeFromString(it) } catch (e: Exception) { null }
        }

        ParagraphResultResponseDto(
            userId = updatedRow[SpeakingParagraphResultsTable.userId].value,
            paragraphId = updatedRow[SpeakingParagraphResultsTable.paragraphId].value,
            wordEvaluation = parsedEvaluation,
            goodCount = updatedRow[SpeakingParagraphResultsTable.goodCount],
            mediumCount = updatedRow[SpeakingParagraphResultsTable.mediumCount],
            badCount = updatedRow[SpeakingParagraphResultsTable.badCount],
            userAudioUrl = updatedRow[SpeakingParagraphResultsTable.userAudioUrl]
        )

    }
}