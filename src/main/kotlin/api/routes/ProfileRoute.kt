package api.routes

import api.models.dto.*
import api.models.enum.CloudinaryFolder
import api.services.CloudinaryService
import api.services.ProfileService
import api.utils.getUserIdOrRespond
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

fun Route.profileRoutes(service: ProfileService) {

    secureRoute("/api/profile") {

        // ===== GET MY PROFILE =====
        get {
            val userId = call.getUserIdOrRespond() ?: return@get

            val profile = service.getProfile(userId)
            call.respond(HttpStatusCode.OK, successResponse(profile, "Lấy hồ sơ thành công"))
        }

        // ===== UPDATE PROFILE =====
        patch {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<UpdateProfileRequest>()

            val success = service.updateProfile(userId, request)

            call.respond(
                if (success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                if (success)
                    successResponse(null, "Cập nhật thông tin thành công")
                else
                    errorResponse("Cập nhật thất bại")
            )
        }
        patch("/fcm-token") {
            val userId = call.getUserIdOrRespond() ?: return@patch
            val request = call.receive<UpdateFcmTokenRequest>()

            val success = service.updateFcmToken(userId, request)

            call.respond(
                if (success) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
                if (success)
                    successResponse(null, "Cập nhật FCM Token thành công")
                else
                    errorResponse("Không thể cập nhật FCM Token")
            )
        }
        patch("/avatar") {
            val userId = call.getUserIdOrRespond() ?: return@patch

            val isDeletion = call.request.queryParameters["action"] == "delete"

            val multipart = call.receiveMultipart()
            var avatarImage: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val fileBytes = part.provider().readRemaining().readByteArray()
                        when (part.name) {
                            "avatar" -> avatarImage = fileBytes
                        }
                        part.dispose()
                    }

                    else -> part.dispose()
                }
            }

            var cloudAvatarImageUrl: String? = null

            if (!isDeletion) {
                if (avatarImage == null) {
                    call.respond(HttpStatusCode.BadRequest, "Thiếu dữ liệu ảnh")
                }

                avatarImage!!.let { bytes ->
                    val savedPath =
                        CloudinaryService.uploadImage(bytes, CloudinaryFolder.PROFILE.path)
                    cloudAvatarImageUrl = savedPath
                }
            }

            val isSuccess = service.uploadAvatar(userId, cloudAvatarImageUrl)

            if (isSuccess) {
                call.respond(
                    HttpStatusCode.OK,
                    successResponse(isSuccess, "Cập nhật ảnh đại diện thành công")
                )
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    errorResponse("Cập nhật ảnh đại diện thất bại")
                )
            }
        }
        get("/achievements") {
            val userId = call.getUserIdOrRespond() ?: return@get
            val result = service.getAchievement(userId)
            call.respond(HttpStatusCode.OK, successResponse(result, "Lấy thành tựu thành công"))
        }
    }
}