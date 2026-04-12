package api.routes

import api.models.dto.*
import api.models.enum.UserRole
import api.services.SpeakingDayService
import api.utils.getLongParamOrRespond
import api.utils.getUserIdOrRespond
import api.utils.handleResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.speakingDayRoutes(service: SpeakingDayService) {

    // ===== PUBLIC / USER =====
    secureRoute("/api/speaking-day/{speakingDayId}") {

        get {
            val speakingDayId = call.getLongParamOrRespond("speakingDayId") ?: return@get

            val data = service.getParagraphSpeakingDay(speakingDayId)
            call.respond(
                HttpStatusCode.OK,
                successResponse(data, "Lấy danh sách paragraph thành công")
            )
        }
    }

    // ===== TEACHER ONLY =====
    secureRoute("/api/users/me/speaking-day", listOf(UserRole.TEACHER)) {

        // CREATE
        post {
            val userId = call.getUserIdOrRespond() ?: return@post
            val request = call.receive<CreateSpeakingDayRequest>()

            service.addSpeakingDay(userId, request).handleResult(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.Created,
                        successResponse(it, "Tạo bài học thành công")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        // UPDATE
        patch("/{speakingDayId}") {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val speakingDayId = call.getLongParamOrRespond("speakingDayId") ?: return@patch
            val request = call.receive<EditSpeakingDayRequest>()

            service.editSpeakingDay(userId, speakingDayId, request).handleResult(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(null, "Cập nhật bài học thành công")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.Forbidden, errorResponse(it))
                }
            )
        }

        // DELETE
        delete("/{speakingDayId}") {
            val userId = call.getUserIdOrRespond() ?: return@delete
            val speakingDayId = call.getLongParamOrRespond("speakingDayId") ?: return@delete

            service.deleteSpeakingDay(userId, speakingDayId).handleResult(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(null, "Xóa bài học thành công")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.Forbidden, errorResponse(it))
                }
            )
        }

        // REORDER PARAGRAPHS
        patch("/{speakingDayId}/paragraphs/reorder") {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val speakingDayId = call.getLongParamOrRespond("speakingDayId") ?: return@patch
            val request = call.receive<ReorderParagraphsRequest>()

            service.reorderParagraphs(userId, speakingDayId, request).handleResult(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(null, "Cập nhật thứ tự thành công")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }
    }
}