package api.routes

import api.models.dto.CreateParagraphRequest
import api.models.dto.DeleteParagraphRequest
import api.models.dto.UpdateParagraphRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.ParagraphService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.paragraphRoute(paragraphService: ParagraphService) {

    route("/api/paragraph") {
        // Mở comment authenticate khi bạn đã setup auth-jwt
        // authenticate("auth-jwt") {
        post {
            try {
                // TODO: Lấy role thực tế từ Token thay vì hardcode
                // val userRole = call.getUserRole()
                val userRole = "teacher"

                // Parsing body từ JSON sang DTO
                val request = call.receive<CreateParagraphRequest>()

                // Gọi service
                val result = paragraphService.createParagraph(request, userRole)

                result.fold(
                    onSuccess = { data ->
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(data, "Create paragraph successfully")
                        )
                    },
                    onFailure = { exception ->
                        when (exception.message) {
                            "FORBIDDEN_ROLE" -> call.respond(
                                HttpStatusCode.Forbidden,
                                errorResponse("Quyền truy cập bị từ chối: Chỉ giáo viên mới có thể tạo paragraph.")
                            )
                            else -> call.respond(
                                HttpStatusCode.BadRequest,
                                errorResponse(exception.message ?: "Có lỗi xảy ra khi tạo paragraph")
                            )
                        }
                    }
                )
            } catch (e: ContentTransformationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    errorResponse("Dữ liệu đầu vào sai định dạng")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    errorResponse("Lỗi hệ thống: ${e.localizedMessage}")
                )
            }
        }

        patch("/{paragraphId}/info") {
            try {
                val userRole = "teacher" // TODO: Lấy từ Auth Token

                val idString = call.parameters["paragraphId"]
                if (idString == null) {
                    call.respond(HttpStatusCode.BadRequest, errorResponse("Thiếu paragraphId"))
                    return@patch
                }
                val paragraphId = idString.toLong()

                val request = call.receive<UpdateParagraphRequest>()
                val result = paragraphService.updateParagraphInfo(paragraphId, request, userRole)

                result.fold(
                    onSuccess = { data ->
                        call.respond(
                            HttpStatusCode.OK,
                            // Ghi chú: Đổi message để phù hợp ngữ cảnh Update
                            successResponse(data, "Update paragraph info successfully")
                        )
                    },
                    onFailure = { exception ->
                        if (exception.message == "FORBIDDEN_ROLE") {
                            call.respond(HttpStatusCode.Forbidden, errorResponse("Chỉ giáo viên mới có quyền cập nhật."))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, errorResponse(exception.message ?: "Lỗi cập nhật"))
                        }
                    }
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, errorResponse("Lỗi hệ thống: ${e.localizedMessage}"))
            }
        }

        delete("/{paragraphId}") {
            try {
                val userRole = "teacher" // TODO: Lấy từ Auth Token

                val idString = call.parameters["paragraphId"]
                if (idString == null) {
                    call.respond(HttpStatusCode.BadRequest, errorResponse("Thiếu paragraphId"))
                    return@delete
                }
                val paragraphId = idString.toLong()

                val request = call.receive<DeleteParagraphRequest>()
                val result = paragraphService.deleteParagraph(paragraphId, request, userRole)

                result.fold(
                    onSuccess = {
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(null, "Delete paragraph successfully")
                        )
                    },
                    onFailure = { exception ->
                        if (exception.message == "FORBIDDEN_ROLE") {
                            call.respond(HttpStatusCode.Forbidden, errorResponse("Chỉ giáo viên mới có quyền xóa."))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, errorResponse(exception.message ?: "Lỗi khi xóa"))
                        }
                    }
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, errorResponse("Lỗi hệ thống: ${e.localizedMessage}"))
            }
        }
    }
}