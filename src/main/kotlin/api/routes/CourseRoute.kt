package api.routes

import api.models.dto.*
import api.models.enum.UserRole
import api.utils.getLongParamOrRespond
import api.utils.getUserIdOrRespond
import api.utils.handleResult
import com.lexa.api.services.CoursesService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.courseRoutes(service: CoursesService) {

    // ===== PUBLIC (BUT NEED AUTH USER CONTEXT) =====
    secureRoute("/api/courses") {

        get("/featured") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getFeaturedCourses(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Danh sách khóa học nổi bật"))
        }

        get("/studying") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getStudyingCourses(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Danh sách đang học"))
        }

        get("/top-studied") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getTopStudiedCourses(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Top khóa học"))
        }

        get {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getAllCourses(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Tất cả khóa học"))
        }

        // ===== FAVORITE =====
        post("/{courseId}/favorite") {
            val userId = call.getUserIdOrRespond() ?: return@post
            val courseId = call.getLongParamOrRespond("courseId") ?: return@post

            service.favoriteCourse(userId, courseId).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Đã thêm yêu thích"))
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }

        delete("/{courseId}/favorite") {
            val userId = call.getUserIdOrRespond() ?: return@delete
            val courseId = call.getLongParamOrRespond("courseId") ?: return@delete

            service.disFavoriteCourse(userId, courseId).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Đã bỏ yêu thích"))
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }

        // ===== COURSE DETAIL (speaking days) =====
        get("/{courseId}/speaking-days") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val courseId = call.getLongParamOrRespond("courseId") ?: return@get

            val data = service.getSpeakingDayCourse(userId, courseId)

            call.respond(
                if (data != null) HttpStatusCode.OK else HttpStatusCode.NotFound,
                if (data != null)
                    successResponse(data, "Chi tiết khóa học")
                else
                    errorResponse("Không tìm thấy hoặc private")
            )
        }
    }

    // ===== USER FAVORITE LIST =====
    secureRoute("/api/user/me/course/favorite") {

        get {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getFavoriteCourses(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Danh sách yêu thích"))
        }
    }

    // ===== TEACHER ONLY =====
    secureRoute("/api/users/me/courses", listOf(UserRole.TEACHER)) {

        get {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getMyCourses(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Khóa học của tôi"))
        }

        post {
            val userId = call.getUserIdOrRespond() ?: return@post
            val request = call.receive<CreateCourseRequest>()

            service.addCourse(userId, request).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.Created, successResponse(it, "Tạo khóa học thành công"))
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        patch("/{courseId}") {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val courseId = call.getLongParamOrRespond("courseId") ?: return@patch
            val request = call.receive<EditCourseRequest>()

            service.editCourse(userId, courseId, request).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Chỉnh sửa thành công"))
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        delete("/{courseId}") {
            val userId = call.getUserIdOrRespond() ?: return@delete
            val courseId = call.getLongParamOrRespond("courseId") ?: return@delete

            service.deleteCourse(userId, courseId).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Xóa thành công"))
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }
    }
}