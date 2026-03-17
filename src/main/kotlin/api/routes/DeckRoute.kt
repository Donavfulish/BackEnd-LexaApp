package api.routes

import api.models.dto.CreateDeckRequest
import api.models.dto.UpdateDeckRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.DeckService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.deckRoutes(deckService: DeckService) {

    route("/api/decks") {

        get {
            val decks = deckService.getAllDecks()
            call.respond(
                HttpStatusCode.OK,
                successResponse(decks, "Lấy danh sách bộ từ vựng thành công")
            )
        }
    }

    route("/api/my-decks") {
        get {
            val userId = call.request.queryParameters["userId"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    errorResponse("Thiếu userId")
                )

            val decks = deckService.getMyDecks(userId)

            call.respond(
                HttpStatusCode.OK,
                successResponse(decks, "Lấy danh sách bộ từ vựng cá nhân thành công")
            )
        }

        post {
            val request = call.receive<CreateDeckRequest>()

            deckService.addDeck(request)
                .onSuccess { id ->
                    call.respond(
                        HttpStatusCode.Created,
                        successResponse(mapOf("id" to id), "Tạo bộ từ vựng thành công")
                    )
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        errorResponse(error.message ?: "Lỗi khi tạo bộ từ vựng")
                    )
                }
        }

        // UPDATE Deck
        put{
            val request = call.receive<UpdateDeckRequest>()

            deckService.updateDeck(request)
                .onSuccess { success ->
                    if (success) {
                        call.respond(HttpStatusCode.OK, successResponse(null, "Cập nhật thành công"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, errorResponse("Không tìm thấy bộ từ vựng"))
                    }
                }
                .onFailure { error ->
                    call.respond(HttpStatusCode.BadRequest, errorResponse(error.message ?: "Lỗi cập nhật"))
                }
        }

        // DELETE Deck
        delete("/{id}") {
            val id = call.parameters["id"]!!.toLong()

            deckService.deleteDeck(id)
                .onSuccess { success ->
                    if (success) {
                        call.respond(HttpStatusCode.OK, successResponse(null, "Xóa thành công"))
                    } else {
                        call.respond(HttpStatusCode.NotFound, errorResponse("Không tìm thấy bộ từ vựng"))
                    }
                }
                .onFailure { error ->
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(error.message ?: "Lỗi khi xóa"))
                }
        }
    }
}
