package api.routes

import api.config.getUserId
import api.config.getUserRole
import api.models.dto.errorResponse
import api.models.enum.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.apache.commons.logging.Log

fun Route.secureRoute(
    path: String,
    roles: List<UserRole>? = null,
    build: Route.() -> Unit
): Route {
    return route(path) {
        authenticate("auth-jwt") {
            if (roles != null) {
                intercept(ApplicationCallPipeline.Call) {

                    val roleString = call.getUserRole()
                    val role = roleString?.let {
                        runCatching { UserRole.valueOf(it) }.getOrNull()
                    }

                    if (role == null || role !in roles) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            errorResponse("Bạn không có quyền truy cập: user là $roleString")
                        )
                        finish()
                    }
                }
            }

            build()
        }
    }
}