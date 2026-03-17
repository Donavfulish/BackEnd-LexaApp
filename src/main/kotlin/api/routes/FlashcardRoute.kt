package api.routes

import api.models.dto.CreateFlashcardRequest
import api.models.dto.UpdateFlashcardRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.FlashcardService
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*

fun Route.flashcardRoutes(flashcardService: FlashcardService){
    route("api/{deckId}/flashcards") {
        get {
            val deckId = call.parameters["deckId"]!!.toLong()
            val flashcards = flashcardService.getAllFlashcard(deckId)
            call.respond(
                HttpStatusCode.OK,
                successResponse(flashcards, "Lấy danh sách Flashcard thành công")
            )
        }

        post {
            val request = call.receive<CreateFlashcardRequest>()
            flashcardService.addFlashcard(request)
                .onSuccess { id ->
                    call.respond(
                        HttpStatusCode.Created,
                        successResponse(mapOf("id" to id), "Thêm Flashcard thành công")
                    )
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        errorResponse(error.message ?: "Lỗi khi thêm Flashcard")
                    )
                }
        }

        put {
            val request = call.receive<UpdateFlashcardRequest>()
            flashcardService.updateFlashcard(request)
                .onSuccess { success ->
                    if (success) {
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(null, "Cập nhật Flashcard thành công")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            errorResponse("Không tìm thấy Flashcard để cập nhật")
                        )
                    }
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        errorResponse(error.message ?: "Lỗi khi cập nhật")
                    )
                }
        }

        delete("/{id}") {
            val id = call.parameters["id"]!!.toLong()

            flashcardService.deleteFlashcard(id)
                .onSuccess { success ->
                    if (success) {
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(null, "Xóa Flashcard thành công")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            errorResponse("Không tìm thấy Flashcard để xóa")
                        )
                    }
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        errorResponse(error.message ?: "Lỗi khi xóa")
                    )
                }
        }
    }
}