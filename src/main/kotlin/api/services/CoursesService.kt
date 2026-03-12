package com.lexa.api.services

import api.models.dto.CourseDto
import api.models.dto.CreateCourseRequest
import api.repository.CourseRepository

// Nơi xử lí logic nghiệp vụ, cầu nối giữa route và repo, tương tự controller
class CoursesServices (
    private val courseRepository: CourseRepository
) {
    suspend fun getCourses(): List<CourseDto> {
        return courseRepository.getAllCourses()
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