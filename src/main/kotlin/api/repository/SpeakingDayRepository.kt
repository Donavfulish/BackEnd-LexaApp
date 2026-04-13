package api.repository

import api.models.dto.CreateSpeakingDayRequest
import api.models.dto.EditSpeakingDayRequest
import api.models.dto.ReorderParagraphsRequest
import api.models.dto.ShortParagraphDto
import api.models.dto.ShortParagraphSpeakingDayDto
import api.models.dto.ShortSpeakingDayDto
import api.models.dto.SpeakingDayPagination
import api.models.tables.CoursesTable
import api.models.tables.SpeakingDaysTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.innerJoin
class SpeakingDayRepository {
    suspend fun getParagraphSpeakingDay(speakingDayId: Long): ShortParagraphSpeakingDayDto? = dbQuery {
        SpeakingDaysTable
        .select { SpeakingDaysTable.id eq speakingDayId }
        .map { row ->
            val paragraphs = SpeakingParagraphsTable
                .select { SpeakingParagraphsTable.speakingDayId eq speakingDayId }
                .orderBy(SpeakingParagraphsTable.paragraphOrder to SortOrder.ASC)
                .map { pRow ->
                    ShortParagraphDto(
                        id = pRow[SpeakingParagraphsTable.id].value,
                        paragraph = pRow[SpeakingParagraphsTable.paragraph],
                        paragraph_order = pRow[SpeakingParagraphsTable.paragraphOrder]
                    )
                }

            ShortParagraphSpeakingDayDto(
                title = row[SpeakingDaysTable.title],
                list_paragraphs = paragraphs
            )
        }
        .singleOrNull()
    }

    suspend fun getSpeakingDays(userId: Int, courseId: Long, nextOrder: Long?): SpeakingDayPagination = dbQuery {
        val baseQuery = SpeakingDaysTable
            .select { (SpeakingDaysTable.courseId eq courseId) }

        val totalItems = baseQuery.count()
        if(nextOrder != null){
            baseQuery.andWhere {
                SpeakingDaysTable.dayOrder greater nextOrder
            }
        }

        val results = baseQuery
            .orderBy(SpeakingDaysTable.dayOrder to SortOrder.ASC)
            .limit(10)
            .map { dayRow ->
                val dayId = dayRow[SpeakingDaysTable.id]

                val totalParaExpr = SpeakingParagraphsTable.id.count()
                val totalParas: Long = SpeakingParagraphsTable
                    .slice(totalParaExpr)
                    .select { SpeakingParagraphsTable.speakingDayId eq dayId }
                    .firstOrNull()
                    ?.get(totalParaExpr) ?: 0L

                val doneParaExpr = SpeakingParagraphResultsTable.paragraphId.count()
                val doneParas: Long = (SpeakingParagraphResultsTable
                    .innerJoin(SpeakingParagraphsTable)
                    .slice(doneParaExpr)
                    .select {
                        (SpeakingParagraphsTable.speakingDayId eq dayId) and
                                (SpeakingParagraphResultsTable.userId eq userId)
                    }
                    .firstOrNull()
                    ?.get(doneParaExpr) ?: 0L)

                val completed = if (totalParas == 0L) 0 else ((doneParas * 100) / totalParas).toInt()

                ShortSpeakingDayDto(
                    speakingDayId = dayRow[SpeakingDaysTable.id].value,
                    title = dayRow[SpeakingDaysTable.title] ?: "",
                    completed = completed,
                    paragraphNum = totalParas.toInt(),
                    order = dayRow[SpeakingDaysTable.dayOrder]!!
                )
            }
        SpeakingDayPagination(
            data = results,
            totalItems = totalItems.toInt()
        )
    }

    suspend fun addSpeakingDay(userId: Int, speakingDay: CreateSpeakingDayRequest): Long = dbQuery {
        val course = CoursesTable
            .select { CoursesTable.id eq speakingDay.courseId }
            .singleOrNull() ?: throw IllegalArgumentException("Không tìm thấy khóa học này để thêm ngày")

        if (course[CoursesTable.creatorId].value != userId) {
            throw IllegalArgumentException("Bạn không có quyền thêm ngày vào khóa học của người khác")
        }
        val maxOrderExpr = SpeakingDaysTable.dayOrder.max()
        val currentMaxOrder = SpeakingDaysTable
            .slice(maxOrderExpr)
            .select { SpeakingDaysTable.courseId eq speakingDay.courseId }
            .singleOrNull()
            ?.get(maxOrderExpr) ?: 0L
        val nextOrder = currentMaxOrder + 1

        SpeakingDaysTable.insertAndGetId {
            it[this.courseId] = speakingDay.courseId
            it[this.title] = speakingDay.title
            it[this.dayOrder] = nextOrder
        }.value
    }
    suspend fun editSpeakingDay(userId: Int, speakingDayId: Long, speakingDay: EditSpeakingDayRequest): Boolean = dbQuery {
        val speakingDayWithCourse = SpeakingDaysTable
            .innerJoin(CoursesTable, {SpeakingDaysTable.courseId},{ CoursesTable.id})
            .select { SpeakingDaysTable.id eq speakingDayId }
            .singleOrNull() ?: throw IllegalArgumentException("Không tìm thấy bài học này để chỉnh sửa")

        val creatorIdInDb = speakingDayWithCourse[CoursesTable.creatorId].value
        if (creatorIdInDb != userId) {
            throw IllegalArgumentException("Bạn không có quyền chỉnh sửa bài học của người khác")
        }
        val updatedRows = SpeakingDaysTable.update({ SpeakingDaysTable.id eq speakingDayId }) {
            it[this.title] = speakingDay.title
            it[updatedAt] = java.time.LocalDateTime.now()
        }

        updatedRows > 0
    }

    suspend fun deleteSpeakingDay(userId: Int, speakingDayId: Long): Boolean = dbQuery {

        val speakingDayWithCourse = SpeakingDaysTable
            .innerJoin(CoursesTable, {SpeakingDaysTable.courseId}, { CoursesTable.id })
            .select { SpeakingDaysTable.id eq speakingDayId }
            .singleOrNull() ?: throw IllegalArgumentException("Không tìm thấy bài học này để xóa")

        val creatorIdInDb = speakingDayWithCourse[CoursesTable.creatorId].value

        if (creatorIdInDb != userId) {
            throw IllegalArgumentException("Bạn không có quyền xóa bài học của người khác")
        }

        val dayToDelete = SpeakingDaysTable
            .select { SpeakingDaysTable.id eq speakingDayId }
            .singleOrNull() ?: return@dbQuery false

        val targetCourseId = dayToDelete[SpeakingDaysTable.courseId] ?: return@dbQuery false
        val targetOrder = dayToDelete[SpeakingDaysTable.dayOrder] ?: 0L


        val deletedRows = SpeakingDaysTable.deleteWhere { SpeakingDaysTable.id eq speakingDayId }


        if (deletedRows > 0) {
            SpeakingDaysTable.update({
                (SpeakingDaysTable.courseId eq targetCourseId) and
                        (SpeakingDaysTable.dayOrder greater targetOrder)
            }) {
                with(SqlExpressionBuilder) {
                    it.update(dayOrder, dayOrder - 1L)
                }
            }
        }
        deletedRows > 0
    }

    suspend fun reorderParagraphs(userId: Int, speakingDayId: Long, request: ReorderParagraphsRequest): Boolean = dbQuery {
        // 1. Kiểm tra quyền sở hữu bài học
        val speakingDayWithCourse = SpeakingDaysTable
            .innerJoin(CoursesTable, {SpeakingDaysTable.courseId}, { CoursesTable.id })
            .select { SpeakingDaysTable.id eq speakingDayId }
            .singleOrNull() ?: throw IllegalArgumentException("Không tìm thấy bài học này")

        val creatorIdInDb = speakingDayWithCourse[CoursesTable.creatorId].value
        if (creatorIdInDb != userId) {
            throw IllegalArgumentException("Bạn không có quyền chỉnh sửa bài học của người khác")
        }

        // --- TRÁNH LỖI DUPLICATE UNIQUE CONSTRAINT ---

        // BƯỚC 1: Cập nhật tất cả các đoạn văn sang giá trị âm (tạm thời)
        // Mục đích: Dọn chỗ trống, tránh đụng độ với các paragraph_order đang tồn tại
        request.paragraphs.forEach { paragraphToUpdate ->
            SpeakingParagraphsTable.update({
                (SpeakingParagraphsTable.id eq paragraphToUpdate.id) and
                        (SpeakingParagraphsTable.speakingDayId eq speakingDayId)
            }) {
                // Đảo thành số âm
                it[paragraphOrder] = -(paragraphToUpdate.order)
            }
        }

        // BƯỚC 2: Cập nhật lại về giá trị dương (giá trị thật)
        request.paragraphs.forEach { paragraphToUpdate ->
            SpeakingParagraphsTable.update({
                (SpeakingParagraphsTable.id eq paragraphToUpdate.id) and
                        (SpeakingParagraphsTable.speakingDayId eq speakingDayId)
            }) {
                it[paragraphOrder] = paragraphToUpdate.order
                it[updatedAt] = java.time.LocalDateTime.now()
            }
        }

        true
    }
}