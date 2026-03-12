package com.lexa.api.routes

import api.models.dto.ApiResponse
import api.models.dto.CreateCourseRequest
import api.models.dto.successResponse
import com.lexa.api.services.CoursesService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseRoutes(coursesService: CoursesService) {

    route("/api/courses") {

        get {
            val courses = coursesService.getCourses()

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học thành công")
            )
        }

        post {

            val request = call.receive<CreateCourseRequest>()

            val id = coursesService.addCourse(request).getOrThrow()

            call.respond(
                HttpStatusCode.Created,
                mapOf("id" to id)
            )
        }
    }
}