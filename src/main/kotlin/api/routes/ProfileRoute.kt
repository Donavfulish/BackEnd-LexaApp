package api.routes

import api.models.dto.*
import api.services.ProfileService
import api.utils.getUserIdOrRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

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
    }
}