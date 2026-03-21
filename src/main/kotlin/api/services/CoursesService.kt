package com.lexa.api.services

import api.models.dto.ShortCourseDto
import api.models.dto.CreateCourseRequest
import api.repository.CoursesRepository

// Nơi xử lí logic nghiệp vụ, cầu nối giữa route và repo, tương tự controller
class CoursesService (
    private val courseRepository: CoursesRepository
) {
//    suspend fun getCourses(): List<ShortCourseDto> {
////        return courseRepository.getAllCourses()
//    }

    suspend fun getFeaturedCourses(): List<ShortCourseDto> {
        return courseRepository.getFeaturedCourses(10)
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