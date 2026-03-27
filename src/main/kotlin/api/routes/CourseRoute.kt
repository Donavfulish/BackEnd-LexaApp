package com.lexa.api.routes

import api.config.getUserId
import api.models.dto.CreateCourseRequest
import api.models.dto.EditCourseRequest
import io.ktor.server.auth.authenticate
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
            val userId = call.getUserId() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
            )

            val courses = coursesService.getFeaturedCourses(userId)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học nổi bât thành công")
            )
        }


    }
    route("/api/courses/studying") {

        get {
            val userId = call.getUserId() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
            )
            val courses = coursesService.getStudyingCourses(userId)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khóa học đang học thành công")
            )
        }


    }
    route("/api/courses/top-studied") {

        get {
            val userId = call.getUserId() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
            )
            val courses = coursesService.getTopStudiedCourses(userId)

            call.respond(
                HttpStatusCode.OK,
                successResponse(courses, "Lấy danh sách khoá học có nhiều lượt học nhất  thành công")
            )
        }


    }
    route("/api/courses") {
        authenticate("auth-jwt") { // Bọc phương thức trong hàm này để xác thực token
            get {
                val userId = call.getUserId() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )
                val courses = coursesService.getAllCourses(userId)

                call.respond(
                    HttpStatusCode.OK,
                    successResponse(courses, "Lấy danh sách tất cả khoá học  thành công")
                )
            }
            post {
                val userId = call.getUserId() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val course = call.receive<CreateCourseRequest>()
                val courseResult = coursesService.addCourse(userId,course )
                courseResult.fold(
                    onSuccess = { courseId ->
                        call.respond(
                            HttpStatusCode.Created,
                            successResponse(courseId, "Tạo khóa học thành công")
                        )
                    },
                    onFailure = { exception ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            errorResponse(exception.message ?: "Đã có lỗi xảy ra khi tạo khóa học")
                        )
                    }
                )
            }
            post ("/{courseId}/favorite"){
                val userId = call.getUserId() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val courseId = call.parameters["courseId"]?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID khóa học không hợp lệ")
                )

                coursesService.favoriteCourse(userId, courseId).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, successResponse(null, "Yêu thích khóa học thành công."))
                    },
                    onFailure = {
                        call.respond(HttpStatusCode.InternalServerError, errorResponse("Lỗi hệ thống"))
                    }
                )
            }
            delete("/{courseId}/favorite") {
                val userId = call.getUserId() ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val courseId = call.parameters["courseId"]?.toLongOrNull() ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID khóa học không hợp lệ")
                )

                coursesService.disFavoriteCourse(userId, courseId).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, successResponse(null, "Bỏ yêu thích khóa học thành công."))
                    },
                    onFailure = {
                        call.respond(HttpStatusCode.InternalServerError, errorResponse("Lỗi hệ thống"))
                    }
                )
            }
        }
    }

    route("/api/user/me/course/favorite") {
        authenticate("auth-jwt") { // Bọc phương thức trong hàm này để xác thực token
            get() {
                val userId = call.getUserId() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )
                val courses = coursesService.getFavoriteCourses(userId)

                call.respond(
                    HttpStatusCode.OK,
                    successResponse(courses, "Lấy danh sách khóa yêu thích thành công")
                )
            }
        }
    }

    route("/api/users/me/courses") {

        authenticate("auth-jwt") { // Bọc phương thức trong hàm này để xác thực token
            get {
                val userId = call.getUserId() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )
                val courses = coursesService.getMyCourses(userId)

                call.respond(
                    HttpStatusCode.OK,
                    successResponse(courses, "Lấy danh sách khóa học của tôi  thành công")
                )
            }
            patch("/{courseId}") {
                val userId = call.getUserId() ?: return@patch call.respond(
                    HttpStatusCode.Unauthorized,
                    errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )
                val courseId = call.parameters["courseId"]?.toLongOrNull() ?: return@patch call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID khóa học không hợp lệ")
                )

                val course = call.receive<EditCourseRequest>()
                val courseResult = coursesService.editCourse(userId,courseId, course )
                courseResult.fold(
                    onSuccess = { courseId ->
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(null, "Chỉnh sửa khóa học thành công")
                        )
                    },
                    onFailure = { exception ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            errorResponse(exception.message ?: "Đã có lỗi xảy ra khi chỉnh sửa khóa học")
                        )
                    }
                )
            }
            delete  ("/{courseId}") {

                val userId = call.getUserId() ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized,
                    errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val courseId = call.parameters["courseId"]?.toLongOrNull() ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID khóa học không hợp lệ")
                )

                val courseResult = coursesService.deleteCourse(userId,courseId )
                courseResult.fold(
                    onSuccess = { courseId ->
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(null, "Xóa khóa học thành công")
                        )
                    },
                    onFailure = { exception ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            errorResponse(exception.message ?: "Đã có lỗi xảy ra khi xóa khóa học")
                        )
                    }
                )
            }
        }

    }
    route("/api/courses/{courseId}/speaking-days") {
        authenticate("auth-jwt") {
            get {
                val userId = call.getUserId()
                val courseId: Long = call.parameters["courseId"]!!.toLong()
                val courses = coursesService.getSpeakingDayCourse(userId!!, courseId)

                if (courses == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        errorResponse("Không tìm thấy khóa học hoặc khóa học đang ở chế độ riêng tư")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(courses, "Lấy chi tiết khóa học thành công")
                    )
                }
            }
        }
    }
}
