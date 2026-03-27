package api.routes

import api.config.getUserId
import api.models.dto.CreateFlashcardRequest
import api.models.dto.UpdateFlashcardRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.FlashcardService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*

fun Route.flashcardRoutes(flashcardService: FlashcardService){
    route("api/decks/{deckId}/flashcards") {
        authenticate("auth-jwt") {
            get {
                val deckId = call.parameters["deckId"]!!.toLong()
                val flashcards = flashcardService.getAllFlashcard(deckId)
                call.respond(
                    HttpStatusCode.OK,
                    successResponse(flashcards, "Lấy danh sách Flashcard thành công")
                )
            }

            post {
                val userId = call.getUserId()
                val request = call.receive<CreateFlashcardRequest>()
                flashcardService.addFlashcard(userId!!, request)
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

            patch {
                val userId = call.getUserId()
                val request = call.receive<UpdateFlashcardRequest>()
                flashcardService.updateFlashcard(userId!!, request)
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
                val userId = call.getUserId()
                val id = call.parameters["id"]!!.toLong()

                flashcardService.deleteFlashcard(userId!!, id)
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
}