package api.services

import api.models.dto.CreateCourseRequest
import api.models.dto.ShortCourseDto
import api.models.dto.ShortParagraphSpeakingDayDto
import api.models.dto.SpeakingCourseDetailDto
import api.repository.CoursesRepository
import api.repository.SpeakingDayRepository
import com.lexa.api.config.DatabaseFactory.dbQuery

class SpeakingDayService (
    private val speakingDayRepository: SpeakingDayRepository
) {
    suspend fun getParagraphSpeakingDay(speakingDayId: Long): List<ShortParagraphSpeakingDayDto> {
        return speakingDayRepository.getParagraphSpeakingDay(speakingDayId)

    }
}