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
            val courses = coursesService.getFeaturedCourses(10)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học nổi bât thành công")
            )
        }


    }
    route("/api/courses/studying") {

        get {
            val courses = coursesService.getStudyingCourses(10)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học đang học thành công")
            )
        }


    }
    route("/api/courses/top-studied") {

        get {
            val courses = coursesService.getTopStudiedCourses(10)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khoá học có nhiều lượt học nhất  thành công")
            )
        }


    }
    route("/api/courses") {

        get {
            val courses = coursesService.getAllCourses(10)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách tất cả khoá học  thành công")
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

    // TODO: CHINH LAI ROUTE
    route("/api/users/me/courses") {

        get {
            val courses = coursesService.getMyCourses(10)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học của tôi  thành công")
            )
        }


    }
    route("/api/courses/{courseId}/speaking-days") {

        get {
            val courseId: Long = call.parameters["courseId"]!!.toLong()
            val courses = coursesService.getSpeakingDayCourses(10, courseId)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học speaking day thành công")
            )
        }


    }
}
