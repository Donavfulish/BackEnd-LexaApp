package api.services

import api.models.dto.ShortCourseDto
import api.models.dto.CreateCourseRequest
import api.models.dto.EditCourseRequest
import api.models.dto.GetFeaturedCourseResponse
import api.models.dto.GetStudyingCourseResponse
import api.models.dto.SpeakingCourseDetailDto
import api.repository.CoursesRepository

// Nơi xử lí logic nghiệp vụ, cầu nối giữa route và repo, tương tự controller
class CoursesService (
    private val courseRepository: CoursesRepository
) {


    suspend fun getFeaturedCourses(userId: Int): List<GetFeaturedCourseResponse> {
        return courseRepository.getFeaturedCourses(userId)
    }
    suspend fun getAllCourses(userId: Int): List<ShortCourseDto> {
        return courseRepository.getAllCourses(userId)
    }
    suspend fun getSpeakingDayCourse(userId: Int, courseId: Long): SpeakingCourseDetailDto? {
        return courseRepository.getSpeakingDayCourse(userId, courseId)
    }
    suspend fun getTopStudiedCourses(userId: Int): List<GetFeaturedCourseResponse> {
        return courseRepository.getTopStudiedCourses(userId)
    }
    suspend fun getStudyingCourses(userId: Int): List<GetStudyingCourseResponse> {
        return courseRepository.getStudyingCourses(userId)
    }

    suspend fun getMyCourses(userId: Int): List<ShortCourseDto> {
        return courseRepository.getMyCourses(userId)
    }


    suspend fun getFavoriteCourses(userId: Int): List<ShortCourseDto> {
        return courseRepository.getFavoriteCourses(userId)
    }



    suspend fun addCourse(userId: Int, course: CreateCourseRequest) : Result<Long> {
        if (course.title.isBlank()) {
            return Result.failure(Exception("Tên khóa học không được để trống"))
        }
        return try {
            val id = courseRepository.createCourse(userId, course);
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editCourse(userId: Int, courseId: Long , course: EditCourseRequest) : Result<Boolean> {
        if (course.title.isBlank()) {
            return Result.failure(Exception("Tên khóa học không được để trống"))
        }

        return try {
            val result = courseRepository.editCourse(courseId, userId,course);
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun deleteCourse(userId: Int, courseId: Long ) : Result<Boolean> {
        return try {
            val result = courseRepository.deleteCourse(courseId, userId);
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun favoriteCourse(userId: Int, courseId: Long): Result<Boolean> {
        return try {
            val result = courseRepository.addFavoriteCourse(userId, courseId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disFavoriteCourse(userId: Int, courseId: Long): Result<Boolean> {
        return try {
            val result = courseRepository.removeFavoriteCourse(userId, courseId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}