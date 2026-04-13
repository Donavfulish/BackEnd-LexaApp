package api.routes

import api.models.dto.*
import api.models.enum.CloudinaryFolder
import api.services.CloudinaryService
import api.services.FlashcardService
import api.utils.getLongParamOrRespond
import api.utils.getUserIdOrRespond
import api.utils.handleResult
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

fun Route.flashcardRoutes(service: FlashcardService) {

    secureRoute("/api/decks/{deckId}/flashcards") {

        // ===== GET ALL =====
        get {
            val deckId = call.getLongParamOrRespond("deckId") ?: return@get
            val queryParams = call.request.queryParameters

            val searchInfo = SearchInfo(
                query = queryParams["query"] ?: "",
                sortBy = if(!queryParams["sort"].isNullOrEmpty()) queryParams["sort"] else  "",
                order = if(!queryParams["order"].isNullOrEmpty()) queryParams["order"] else "desc",
                limit = queryParams["limit"]?.toIntOrNull() ?: 10
            )
            val nextCursor = queryParams["next_id"]?.toLongOrNull()
            val data = service.getAllFlashcard(deckId, searchInfo, nextCursor)
            call.respond(HttpStatusCode.OK, successResponse(data, "Lấy danh sách flashcard"))
        }

        // ===== CREATE =====
        post {
            val userId = call.getUserIdOrRespond() ?: return@post

            val multipart = call.receiveMultipart()
            var createRequest: CreateFlashcardRequest? = null
            var flashcardImageBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "data") {
                            val jsonString = part.value
                            createRequest = Json.decodeFromString<CreateFlashcardRequest>(jsonString)
                        }
                    }
                    is PartData.FileItem -> {
                        val fileBytes = part.provider().readRemaining().readByteArray()
                        when (part.name) {
                            "flashcardImage" -> flashcardImageBytes = fileBytes
                        }
                        part.dispose()
                    }
                    else -> part.dispose()
                }
            }

            if (createRequest == null) {
                call.respond(HttpStatusCode.BadRequest, "Thiếu dữ liệu tạo mới")
            }

            // Xử lý đăng ảnh và cập nhật link ảnh
            flashcardImageBytes?.let { bytes ->
                val savedPath = CloudinaryService.uploadImage(bytes, CloudinaryFolder.FLASHCARD.path)
                createRequest!!.imageUrl = savedPath
            }

            service.addFlashcard(userId, createRequest!!).handleResult(
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

            val multipart = call.receiveMultipart()
            var updateRequest: UpdateFlashcardRequest? = null
            var flashcardImageBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "data") {
                            val jsonString = part.value
                            updateRequest = Json.decodeFromString<UpdateFlashcardRequest>(jsonString)
                        }
                    }
                    is PartData.FileItem -> {
                        val fileBytes = part.provider().readRemaining().readByteArray()
                        when (part.name) {
                            "flashcardImage" -> flashcardImageBytes = fileBytes
                        }
                        part.dispose()
                    }
                    else -> part.dispose()
                }
            }

            if (updateRequest == null) {
                call.respond(HttpStatusCode.BadRequest, "Thiếu dữ liệu cập nhật")
            }

            // Xử lý đăng ảnh và cập nhật link ảnh
            flashcardImageBytes?.let { bytes ->
                val savedPath = CloudinaryService.uploadImage(bytes, CloudinaryFolder.FLASHCARD.path)
                updateRequest!!.imageUrl = savedPath
            }

            service.updateFlashcard(userId, updateRequest!!).handleResult(
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