package com.lexa.api.services

import api.models.dto.ShortCourseDto
import api.models.dto.CreateCourseRequest
import api.models.dto.SpeakingCourseDetailDto
import api.repository.CoursesRepository

// Nơi xử lí logic nghiệp vụ, cầu nối giữa route và repo, tương tự controller
class CoursesService (
    private val courseRepository: CoursesRepository
) {


    suspend fun getFeaturedCourses(userId: Int): List<ShortCourseDto> {
        return courseRepository.getFeaturedCourses(userId)
    }
    suspend fun getAllCourses(userId: Int): List<ShortCourseDto> {
        return courseRepository.getAllCourses(userId)
    }
    suspend fun getSpeakingDayCourses(userId: Int, courseId: Long): List<SpeakingCourseDetailDto> {
        return courseRepository.getSpeakingDayCourses(userId, courseId)
    }
    suspend fun getTopStudiedCourses(userId: Int): List<ShortCourseDto> {
        return courseRepository.getTopStudiedCourses(userId)
    }
    suspend fun getStudyingCourses(userId: Int): List<ShortCourseDto> {
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
}