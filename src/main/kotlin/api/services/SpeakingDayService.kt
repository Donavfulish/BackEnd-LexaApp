package api.services

import api.models.dto.CreateCourseRequest
import api.models.dto.CreateSpeakingDayRequest
import api.models.dto.EditSpeakingDayRequest
import api.models.dto.ShortCourseDto
import api.models.dto.ShortParagraphSpeakingDayDto
import api.models.dto.CourseDetailDto
import api.models.dto.SpeakingDayPagination
import api.repository.CoursesRepository
import api.repository.SpeakingDayRepository
import com.lexa.api.config.DatabaseFactory.dbQuery

class SpeakingDayService (
    private val speakingDayRepository: SpeakingDayRepository
) {
    suspend fun getParagraphSpeakingDay(speakingDayId: Long): ShortParagraphSpeakingDayDto? {
        return speakingDayRepository.getParagraphSpeakingDay(speakingDayId)
    }

    suspend fun getSpeakingDays(userId: Int, courseId: Long, nextOrder: Long?): SpeakingDayPagination{
        return speakingDayRepository.getSpeakingDays(userId, courseId, nextOrder)
    }

    suspend fun addSpeakingDay(userId: Int, speakingDay: CreateSpeakingDayRequest): Result<Long> {
        if(speakingDay.title != null){
            if (speakingDay.title.isBlank()) {
                return Result.failure(IllegalArgumentException("Tên bài học không được để trống"))
            }
        }

        return try {
            val newDayId = speakingDayRepository.addSpeakingDay(userId, speakingDay)
            Result.success(newDayId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editSpeakingDay(userId: Int, speakingDayId: Long, speakingDay: EditSpeakingDayRequest): Result<Boolean> {
        if(speakingDay.title != null){
            if (speakingDay.title.isBlank()) {
                return Result.failure(IllegalArgumentException("Tên bài học không được để trống"))
            }
        }

        return try {
            val isUpdated = speakingDayRepository.editSpeakingDay(userId, speakingDayId, speakingDay)

            if (isUpdated) {
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Không tìm thấy bài học này để chỉnh sửa"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSpeakingDay(userId: Int, speakingDayId: Long): Result<Boolean> {
        return try {


            val isDeleted = speakingDayRepository.deleteSpeakingDay(userId, speakingDayId)

            if (isDeleted) {
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Không tìm thấy bài học này để xóa"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}