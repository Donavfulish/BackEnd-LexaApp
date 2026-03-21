package com.lexa.api.routes

import api.models.dto.ApiResponse
import api.models.dto.CreateCourseRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import com.lexa.api.services.CoursesService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.courseRoutes(coursesService: CoursesService) {

    route("/api/courses/featured") {

        get {
            val courses = coursesService.getFeaturedCourses()

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học nổi bât thành công")
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
    route("/api/user/me/course/favorite") {

        get("/{id}") {
            val idString = call.parameters["id"]
            if(idString == null) {
                call.respond(HttpStatusCode.BadRequest,errorResponse("Thiếu id"))
                return@get
            }
            val id: Int = idString.toInt()
            val courses = coursesService.getFavoriteCourses(id)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa yêu thích thành công")
            )
        }
    }



}
