package api.routes


import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.ProfileService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.profileRoutes(profileService: ProfileService) {

    route("/api/profile/{id}") {
        get {
            val idString = call.parameters["id"]
            if(idString == null) {
                call.respond(HttpStatusCode.BadRequest,errorResponse("Thiếu id"))
                return@get
            }
            val id: Int = idString.toInt()
            val profile = profileService.getProfile(id)
            call.respond(
                HttpStatusCode.OK,
                successResponse(profile, "Lấy hồ sơ cá nhân thành công")
            )
        }
    }


}
