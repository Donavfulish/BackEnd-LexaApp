package api.routes

import api.models.dto.*
import api.services.DeckService
import api.utils.getLongParamOrRespond
import api.utils.getUserIdOrRespond
import api.utils.handleResult
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.deckRoutes(service: DeckService) {

    // ===== USER DECKS =====
    secureRoute("/api/user/me/decks") {

        // GET MY DECKS
        get {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getMyDecks(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Lấy danh sách bộ từ vựng"))
        }

        // CREATE
        post {
            val request = call.receive<CreateDeckRequest>()

            service.addDeck(request).handleResult(
                onSuccess = {
                    call.respond(
                        HttpStatusCode.Created,
                        successResponse(it, "Tạo bộ từ vựng thành công")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        // UPDATE
        patch("/{deckId}") {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<UpdateDeckRequest>()

            service.updateDeck(userId, request).handleResult(
                onSuccess = { success ->
                    call.respond(
                        if (success) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        if (success) successResponse(null, "Cập nhật thành công")
                        else errorResponse("Không tìm thấy hoặc không có quyền")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.BadRequest, errorResponse(it))
                }
            )
        }

        // DELETE
        delete("/{deckId}") {
            val userId = call.getUserIdOrRespond() ?: return@delete
            val deckId = call.getLongParamOrRespond("deckId") ?: return@delete

            service.deleteDeck(userId, deckId).handleResult(
                onSuccess = { success ->
                    call.respond(
                        if (success) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        if (success) successResponse(null, "Xóa thành công")
                        else errorResponse("Không tìm thấy bộ từ vựng")
                    )
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }

        // ===== RESULT =====

        get("/result/{deckId}") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val deckId = call.getLongParamOrRespond("deckId") ?: return@get

            service.getDeckResult(userId, deckId).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(it, "Lấy kết quả thành công"))
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }

        post("/result/{deckId}") {
            val userId = call.getUserIdOrRespond() ?: return@post
            val request = call.receive<CreateDeckResultRequest>()

            service.addDeckResult(userId, request).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(it, "Tạo kết quả thành công"))
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }

        patch("/result/{deckId}") {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<UpdateDeckResultRequest>()

            service.updateDeckResult(userId, request).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(it, "Cập nhật kết quả thành công"))
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }
    }

    // ===== FAVORITE LIST =====
    secureRoute("/api/user/me/deck/favorite") {

        get {
            val userId = call.getUserIdOrRespond() ?: return@get
            val data = service.getFavoriteDecks(userId)

            call.respond(HttpStatusCode.OK, successResponse(data, "Danh sách yêu thích"))
        }
    }

    // ===== FAVORITE ACTION =====
    secureRoute("/api/decks") {

        post("/{deckId}/favorite") {
            val userId = call.getUserIdOrRespond() ?: return@post
            val deckId = call.getLongParamOrRespond("deckId") ?: return@post

            service.favoriteDeck(userId, deckId).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Đã thêm yêu thích"))
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }

        delete("/{deckId}/favorite") {
            val userId = call.getUserIdOrRespond() ?: return@delete
            val deckId = call.getLongParamOrRespond("deckId") ?: return@delete

            service.disFavoriteDeck(userId, deckId).handleResult(
                onSuccess = {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Đã bỏ yêu thích"))
                },
                onError = {
                    call.respond(HttpStatusCode.InternalServerError, errorResponse(it))
                }
            )
        }
    }
}