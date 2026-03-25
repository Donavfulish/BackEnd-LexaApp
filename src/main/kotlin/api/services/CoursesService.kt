package com.lexa.api.services

import api.models.dto.ShortCourseDto
import api.models.dto.CreateCourseRequest
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

    suspend fun addCourse(request: CreateCourseRequest) : Result<Long> {
        if (request.title.isBlank()) {
            return Result.failure(Exception("Tên khóa học không được để trống"))
        }

        return try {
            val id = courseRepository.createCourse(request);
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getFavoriteCourses(userId: Int): List<ShortCourseDto> {
        return courseRepository.getFavoriteCourses(userId)
    }

    suspend fun getFavoriteDecks(userId: Int): List<ShortCourseDto> {
        return courseRepository.getFavoriteDecks(userId)
    }
}