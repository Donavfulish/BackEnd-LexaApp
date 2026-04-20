package api.routes

import api.config.getUserRole
import api.models.dto.*
import api.models.enum.UserRole
import api.services.ParagraphService
import api.utils.getLongParamOrRespond
import api.utils.getUserIdOrRespond
import api.utils.handleResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.paragraphRoute(service: ParagraphService) {

    // ===== TEACHER ONLY =====
    secureRoute("/api/paragraph", listOf(UserRole.TEACHER)) {

        // ===== CREATE =====
        post {
            val role = call.getUserRole()
            val request = call.receive<CreateParagraphRequest>()

            service.createParagraph(request, role).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(it, "Create paragraph successfully"))
                },
                onError = {
                    if (it == "FORBIDDEN_ROLE") {
                        call.respond(HttpStatusCode.Forbidden, errorResponse("Chỉ teacher mới được tạo"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                    }
                }
            )
        }

        // ===== UPDATE INFO =====
        patch("/{paragraphId}/info") {
            val role = call.getUserRole()
            val paragraphId = call.getLongParamOrRespond("paragraphId") ?: return@patch
            val request = call.receive<UpdateParagraphRequest>()

            service.updateParagraphInfo(paragraphId, request, role).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(it, "Update paragraph successfully"))
                },
                onError = {
                    if (it == "FORBIDDEN_ROLE") {
                        call.respond(HttpStatusCode.Forbidden, errorResponse("Chỉ teacher mới được cập nhật"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                    }
                }
            )
        }

        // ===== DELETE =====
        delete("/{paragraphId}") {
            val role = call.getUserRole()
            val paragraphId = call.getLongParamOrRespond("paragraphId") ?: return@delete

            service.deleteParagraph(paragraphId, role).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Delete paragraph successfully"))
                },
                onError = {
                    if (it == "FORBIDDEN_ROLE") {
                        call.respond(HttpStatusCode.Forbidden, errorResponse("Chỉ teacher mới được xóa"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                    }
                }
            )
        }
    }

    // ===== STUDENT / USER =====
    secureRoute("/api/paragraph/result") {

        patch {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<UpdateParagraphResultRequest>()

            service.updateParagraphResult(userId, request).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(it, "Update result successfully"))
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        patch("/bulk") {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<SubmitBulkDailyResultRequest>()

            service.submitBulkParagraphResults(userId, request).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(it, "Lưu toàn bộ tiến độ thành công"))
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }
    }
}