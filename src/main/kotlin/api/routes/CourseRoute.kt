package api.routes

import api.models.dto.*
import api.models.enum.CloudinaryFolder
import api.models.enum.UserRole
import api.services.CloudinaryService
import api.utils.getLongParamOrRespond
import api.utils.getUserIdOrRespond
import api.utils.handleResult
import api.services.CoursesService
import api.services.SpeakingDayService
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

fun Route.courseRoutes(service: CoursesService, speakingDayService: SpeakingDayService) {

    // ===== PUBLIC (NO AUTH REQUIRED FOR SOME) =====
    route("/api/topics") {
        get {
            val data = service.getTopics()
            call.respond(HttpStatusCode.OK, successResponse(data, "Danh sách chủ đề"))
        }
    }

    // ===== PUBLIC (BUT NEED AUTH USER CONTEXT) =====
    secureRoute("/api/courses") {
        get("/suggestion"){
            val queryParams = call.request.queryParameters
            val query = queryParams["query"] ?: ""
            val data = service.getCourseSuggestion(query)
            call.respond(HttpStatusCode.OK, successResponse(data, "Tất cả khóa học"))
        }

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
            val queryParams = call.request.queryParameters

            val searchInfo = SearchInfo(
                query = queryParams["query"] ?: "",
                sortBy = if(!queryParams["sort"].isNullOrEmpty()) queryParams["sort"] else  "",
                order = if(!queryParams["order"].isNullOrEmpty()) queryParams["order"] else "desc",
                limit = queryParams["limit"]?.toIntOrNull() ?: 10
            )
            val nextCursor = queryParams["next_id"]?.toLongOrNull()

            val data = service.getAllCourses(userId, searchInfo, nextCursor)

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

        // ===== COURSE DETAIL =====
        get("/{courseId}/course-detail") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val courseId = call.getLongParamOrRespond("courseId") ?: return@get

            val data = service.getCourseDetail(userId, courseId)

            if (data != null) {
                call.respond<ApiResponse<CourseDetailDto>>(
                    HttpStatusCode.OK,
                    successResponse(data, "Chi tiết khóa học")
                )
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    errorResponse("Không tìm thấy hoặc private")
                )
            }
        }

        get("/{courseId}/speaking-days") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val courseId = call.getLongParamOrRespond("courseId") ?: return@get
            val nextOrder = call.request.queryParameters["next_order"]?.toLongOrNull()

            val data = speakingDayService.getSpeakingDays(userId, courseId, nextOrder)

            call.respond(
                HttpStatusCode.OK,
                successResponse(data, "Danh sách bài học")
            )
        }
    }

    // ===== USER FAVORITE LIST =====
    secureRoute("/api/user/me/course") {

        get("/favorite") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val queryParams = call.request.queryParameters

            val searchInfo = SearchInfo(
                query = queryParams["query"] ?: "",
                sortBy = if(!queryParams["sort"].isNullOrEmpty()) queryParams["sort"] else  "",
                order = if(!queryParams["order"].isNullOrEmpty()) queryParams["order"] else "desc",
                limit = queryParams["limit"]?.toIntOrNull() ?: 10
            )
            val nextCursor = queryParams["next_id"]?.toLongOrNull()
            val data = service.getFavoriteCourses(userId, searchInfo, nextCursor)

            call.respond(HttpStatusCode.OK, successResponse(data, "Danh sách yêu thích"))
        }

        get("/learning") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val queryParams = call.request.queryParameters

            val searchInfo = SearchInfo(
                query = queryParams["query"] ?: "",
                sortBy = if(!queryParams["sort"].isNullOrEmpty()) queryParams["sort"] else  "",
                order = if(!queryParams["order"].isNullOrEmpty()) queryParams["order"] else "desc",
                limit = queryParams["limit"]?.toIntOrNull() ?: 10
            )
            val nextCursor = queryParams["next_id"]?.toLongOrNull()
            val data = service.getLearningCourses(userId, searchInfo, nextCursor)

            call.respond(HttpStatusCode.OK, successResponse(data, "Danh sách đang học"))
        }
    }

    // ===== TEACHER ONLY =====
    secureRoute("/api/users/me/courses", listOf(UserRole.TEACHER)) {

        get {
            val userId = call.getUserIdOrRespond() ?: return@get
            val queryParams = call.request.queryParameters

            val searchInfo = SearchInfo(
                query = queryParams["query"] ?: "",
                sortBy = if(!queryParams["sort"].isNullOrEmpty()) queryParams["sort"] else  "",
                order = if(!queryParams["order"].isNullOrEmpty()) queryParams["order"] else "desc",
                limit = queryParams["limit"]?.toIntOrNull() ?: 10
            )
            val nextCursor = queryParams["next_id"]?.toLongOrNull()

            val data = service.getMyCourses(userId, searchInfo, nextCursor)

            call.respond(HttpStatusCode.OK, successResponse(data, "Khóa học của tôi"))
        }

        post {
            val userId = call.getUserIdOrRespond() ?: return@post

            val multipart = call.receiveMultipart()
            var request: CreateCourseRequest? = null
            var courseImageBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "data") {
                            val jsonString = part.value
                            request = Json.decodeFromString<CreateCourseRequest>(jsonString)
                        }
                    }
                    is PartData.FileItem -> {
                        val fileBytes = part.provider().readRemaining().readByteArray()
                        when (part.name) {
                            "courseImage" -> courseImageBytes = fileBytes
                        }
                        part.dispose()
                    }
                    else -> part.dispose()
                }
            }

            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, "Thiếu dữ liệu tạo mới")
            }

            courseImageBytes?.let { bytes ->
                val savedPath = CloudinaryService.uploadImage(bytes, CloudinaryFolder.COURSE.path)
                request!!.thumbnailUrl = savedPath
            }

            service.addCourse(userId, request!!).handleResult(
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

            val multipart = call.receiveMultipart()
            var request: EditCourseRequest? = null
            var courseImageBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "data") {
                            val jsonString = part.value
                            request = Json.decodeFromString<EditCourseRequest>(jsonString)
                        }
                    }
                    is PartData.FileItem -> {
                        val fileBytes = part.provider().readRemaining().readByteArray()
                        when (part.name) {
                            "courseImage" -> courseImageBytes = fileBytes
                        }
                        part.dispose()
                    }
                    else -> part.dispose()
                }
            }

            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, "Thiếu dữ liệu cập nhật")
            }

            courseImageBytes?.let { bytes ->
                val savedPath = CloudinaryService.uploadImage(bytes, CloudinaryFolder.COURSE.path)
                request!!.thumbnailUrl = savedPath
            }

            service.editCourse(userId, courseId, request!!).handleResult(
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
