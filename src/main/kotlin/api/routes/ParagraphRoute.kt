package api.routes

import api.models.dto.CreateParagraphRequest
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
    }
}