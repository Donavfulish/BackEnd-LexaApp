package api.routes

import api.models.dto.*
import api.services.FlashcardService
import api.utils.getLongParamOrRespond
import api.utils.getUserIdOrRespond
import api.utils.handleResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.flashcardRoutes(service: FlashcardService) {

    secureRoute("/api/decks/{deckId}/flashcards") {

        // ===== GET ALL =====
        get {
            val deckId = call.getLongParamOrRespond("deckId") ?: return@get

            val data = service.getAllFlashcard(deckId)
            call.respond(HttpStatusCode.OK, successResponse(data, "Lấy danh sách flashcard"))
        }

        // ===== CREATE =====
        post {
            val userId = call.getUserIdOrRespond() ?: return@post
            val request = call.receive<CreateFlashcardRequest>()

            service.addFlashcard(userId, request).handleResult(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.Created,
                        successResponse(it, "Thêm flashcard thành công")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        // ===== UPDATE =====
        patch {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<UpdateFlashcardRequest>()

            service.updateFlashcard(userId, request).handleResult(
                onSuccess = { success ->
                    call.respond(
                        if (success) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        if (success) successResponse(null, "Cập nhật flashcard thành công")
                        else errorResponse("Không tìm thấy flashcard")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        // ===== DELETE =====
        delete("/{id}") {
            val userId = call.getUserIdOrRespond() ?: return@delete
            val id = call.getLongParamOrRespond("id") ?: return@delete

            service.deleteFlashcard(userId, id).handleResult(
                onSuccess = { success ->
                    call.respond(
                        if (success) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        if (success) successResponse(null, "Xóa flashcard thành công")
                        else errorResponse("Không tìm thấy flashcard")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }
    }

    secureRoute("/api/decks/{deckId}/flashcards/result") {
        get {
            val deckId = call.getLongParamOrRespond("deckId") ?: return@get
            val userId = call.getUserIdOrRespond() ?: return@get

            val data =service.getAllFlashcardWithResult(deckId, userId)
            call.respond(HttpStatusCode.OK, successResponse(data, "Lấy danh sách FC kèm kết quả thành công"))
        }

        // ===== BATCH UPDATE RESULTS =====
        patch {
            val deckId = call.getLongParamOrRespond("deckId") ?: return@patch
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<UpdateFlashcardResultRequest>()

            // Kiểm tra bảo mật: đảm bảo deckId trên URL khớp với dữ liệu gửi lên
            if (deckId != request.deckId) {
                call.respond(HttpStatusCode.BadRequest, errorResponse("Deck ID không khớp"))
                return@patch
            }

            service.updateFlashcardResults(userId, request).handleResult(
                onSuccess = { success ->
                    call.respond(
                        if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                        if (success) successResponse(null, "Cập nhật kết quả flashcard thành công")
                        else errorResponse("Không thể cập nhật kết quả flashcard")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }
    }

}