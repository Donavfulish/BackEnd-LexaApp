package api.routes

import api.config.getUserId
import api.models.dto.CreateDeckRequest
import api.models.dto.CreateDeckResultRequest
import api.models.dto.UpdateDeckRequest
import api.models.dto.UpdateDeckResultRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.DeckService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.deckRoutes(deckService: DeckService) {

    route("/api/user/me/decks") {

        authenticate("auth-jwt") {
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")!!.asInt()
                val decks = deckService.getMyDecks(userId)

                call.respond(
                    HttpStatusCode.OK,
                    successResponse(decks, "Lấy danh sách bộ từ vựng cá nhân thành công")
                )
            }
            get("/result/{deckId}") {
                val deckId = call.parameters["deckId"]!!.toLong()
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")!!.asInt()
                val result = deckService.getDeckResult(userId, deckId)
                result.onSuccess { data ->
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(data, "Lấy kết quả bộ từ vựng thành công")
                    )
                }.onFailure { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        errorResponse(error.message ?: "Lỗi khi lấy kết quả bộ từ vựng")
                    )
                }
            }
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

            patch("/{deckId}"){
                val request = call.receive<UpdateDeckRequest>()
                val userId = call.getUserId()
                deckService.updateDeck(userId!!, request)
                    .onSuccess { success ->
                        if (success) {
                            call.respond(HttpStatusCode.OK, successResponse(null, "Cập nhật thành công"))
                        } else {
                            call.respond(HttpStatusCode.NotFound, errorResponse("Không tìm thấy bộ từ vựng hoặc bạn không có quyền truy cập"))
                        }
                    }
                    .onFailure { error ->
                        call.respond(HttpStatusCode.BadRequest, errorResponse(error.message ?: "Lỗi cập nhật"))
                    }
            }

            delete("/{deckId}") {
                val id = call.parameters["deckId"]!!.toLong()
                val userId = call.getUserId()
                deckService.deleteDeck(userId!!, id)
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

            get("/result/{deckId}") {
                val deckId = call.parameters["deckId"]!!.toLong()
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("id")!!.asInt()
                val result = deckService.getDeckResult(userId, deckId)
                result.onSuccess { data ->
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(data, "Lấy kết quả bộ từ vựng thành công")
                    )
                }.onFailure { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        errorResponse(error.message ?: "Lỗi khi lấy kết quả bộ từ vựng")
                    )
                }
            }

            post("/result/{deckId}") {
                val userId = call.getUserId()
                val request = call.receive<CreateDeckResultRequest>()
                val result = deckService.addDeckResult(userId!!, request)
                result.onSuccess { data ->
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(data, "Tạo kết quả bộ từ vựng thành công")
                    )
                }.onFailure { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        errorResponse(error.message ?: "Lỗi khi tạo kết quả bộ từ vựng")
                    )
                }
            }

            patch("/result/{deckId}"){
                val userId = call.getUserId()
                val request = call.receive<UpdateDeckResultRequest>()
                val result = deckService.updateDeckResult(userId!!, request)
                result.onSuccess { data ->
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(data, "Tạo kết quả bộ từ vựng thành công")
                    )
                }.onFailure { error ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        errorResponse(error.message ?: "Lỗi khi tạo kết quả bộ từ vựng")
                    )
                }
            }
        }

    route("/api/user/me/deck/favorite") {
        authenticate("auth-jwt") { // Bọc phương thức trong hàm này để xác thực token
            get() {
                val userId = call.getUserId() ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val courses = deckService.getFavoriteDecks(userId)

                call.respond(
                    HttpStatusCode.OK,
                    successResponse(courses, "Lấy danh sách khóa yêu thích thành công")
                )
            }
        }
    }
    route("/api/decks") {

        authenticate("auth-jwt") {


            post("/{deckId}/favorite") {
                val userId = call.getUserId() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val deckId = call.parameters["deckId"]?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID bộ từ vựng không hợp lệ")
                )

                deckService.favoriteDeck(userId, deckId).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, successResponse(null, "Yêu thích bộ từ vựng thành công"))
                    },
                    onFailure = {
                        call.respond(HttpStatusCode.InternalServerError, errorResponse("Lỗi hệ thống"))
                    }
                )
            }
            delete("/{deckId}/favorite") {
                val userId = call.getUserId() ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val deckId = call.parameters["deckId"]?.toLongOrNull() ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID bộ từ vựng không hợp lệ")
                )

                deckService.disFavoriteDeck(userId, deckId).fold(
                    onSuccess = {
                        call.respond(HttpStatusCode.OK, successResponse(null, "Bỏ yêu thích bộ từ vựng thành công"))
                    },
                    onFailure = {
                        call.respond(HttpStatusCode.InternalServerError, errorResponse("Lỗi hệ thống"))
                    }
                )
            }
        }
    }
}
