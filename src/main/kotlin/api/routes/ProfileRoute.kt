package api.routes


import api.config.getUserId
import api.models.dto.UpdateProfileRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.ProfileService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.profileRoutes(profileService: ProfileService) {
    authenticate("auth-jwt") {
        route("/api/profile") {
            patch("/update") {
                val id = call.getUserId()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, errorResponse("Thiếu id"))
                    return@patch
                }

                val updatedData = call.receive<UpdateProfileRequest>()

                val isSuccess = profileService.updateProfile(id, updatedData)

                if (isSuccess) {
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(null, "Cập nhật thông tin cá nhân thành công")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        successResponse(null, "Cập nhật thông tin cá nhân thất bại")
                    )
                }

            }

            get("/{id}") {
                val id = call.getUserId()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, errorResponse("Thiếu id"))
                    return@get
                }
                val profile = profileService.getProfile(id)
                call.respond(
                    HttpStatusCode.OK,
                    successResponse(profile, "Lấy hồ sơ cá nhân thành công")
                )
            }
        }
    }
}
