package api.services

import api.events.AppEvent
import api.events.EventBus
import api.models.dto.CreateCourseRequest
import api.models.dto.CreateSpeakingDayRequest
import api.models.dto.EditSpeakingDayRequest
import api.models.dto.ReorderParagraphsRequest
import api.models.dto.ShortCourseDto
import api.models.dto.ShortParagraphSpeakingDayDto
import api.models.dto.CourseDetailDto
import api.models.dto.SpeakingDayPagination
import api.repository.CoursesRepository
import api.repository.ProfileRepository
import api.repository.SpeakingDayRepository
import com.lexa.api.config.DatabaseFactory.dbQuery
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

            coroutineScope {
                val favoritesDeferred = async {
                    ProfileRepository().getUsersWhoFavoritedCourse(userId, speakingDay.courseId)
                }
                val learnersDeferred = async {
                    CoursesRepository().getLearnersInCourse(speakingDay.courseId)
                }
                val nameCourseDeferred = async {
                    CoursesRepository().getCourseDetail(userId,speakingDay.courseId);
                }
                val nameCourse =  nameCourseDeferred.await()?.title;

                val listUsersFavorited = favoritesDeferred.await()
                val listLearners = learnersDeferred.await()

                val combinedUserIds = (listUsersFavorited + listLearners).toSet().toList()

                EventBus.publish(AppEvent.SpeakingDayChanged(
                    combinedUserIds,
                    "Cập nhật khóa học",
                    "Khóa học $nameCourse vừa thêm ngày học"
                ))
            }

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
                coroutineScope {

                    val favoritesDeferred = async {
                        ProfileRepository().getUsersWhoFavoritedCourseBySpeakingDayId(userId, speakingDayId)
                    }
                    val learnersDeferred = async {
                        CoursesRepository().getLearnersInCourseBySpeakingId(speakingDayId)
                    }

                    val listUsersFavorited = favoritesDeferred.await()
                    val listLearners = learnersDeferred.await()

                    val combinedUserIds = (listUsersFavorited + listLearners).toSet().toList()

                    EventBus.publish(AppEvent.SpeakingDayChanged(combinedUserIds, "Cập nhật khóa học", "Khóa học bạn quan tâm vừa chỉnh sửa ngày học"))
                }
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


            val listUsersFavorited = ProfileRepository().getUsersWhoFavoritedCourseBySpeakingDayId(userId, speakingDayId)
            var listLearners = CoursesRepository().getLearnersInCourseBySpeakingId(speakingDayId)
            val combinedUserIds = (listUsersFavorited + listLearners).toSet().toList()

            val isDeleted = speakingDayRepository.deleteSpeakingDay(userId, speakingDayId)

            if (isDeleted) {
                EventBus.publish(AppEvent.SpeakingDayChanged(combinedUserIds, "Cập nhật khóa học", "Khóa học bạn quan tâm xóa ngày học"))
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Không tìm thấy bài học này để xóa"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reorderParagraphs(userId: Int, speakingDayId: Long, request: ReorderParagraphsRequest): Result<Boolean> {
        return try {
            if (request.paragraphs.isEmpty()) {
                return Result.failure(IllegalArgumentException("Danh sách đoạn văn trống"))
            }
            val isUpdated = speakingDayRepository.reorderParagraphs(userId, speakingDayId, request)
            if (isUpdated) {
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("Cập nhật thứ tự thất bại"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}