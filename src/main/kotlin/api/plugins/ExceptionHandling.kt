package api.plugins

import api.models.dto.errorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import org.jetbrains.exposed.exceptions.ExposedSQLException

fun Application.configureExceptionHandling() {

    install(StatusPages) {

        // JSON parse lỗi
        exception<SerializationException> { call, cause ->

            call.application.environment.log.warn(
                "JSON parse error: ${cause.message}"
            )

            call.respond(
                HttpStatusCode.BadRequest,
                errorResponse("Dữ liệu gửi lên không đúng định dạng")
            )
        }

        // Database lỗi
        exception<ExposedSQLException> { call, cause ->

            call.application.environment.log.error(
                "Database error",
                cause
            )

            call.respond(
                HttpStatusCode.Conflict,
                errorResponse("Lỗi dữ liệu hệ thống (có thể do trùng lặp hoặc sai liên kết)")
            )
        }

        // Logic lỗi
        exception<IllegalArgumentException> { call, cause ->

            call.respond(
                HttpStatusCode.BadRequest,
                errorResponse(
                    cause.message ?: "Dữ liệu không hợp lệ"
                )
            )
        }

        // Lỗi không xác định
        exception<Throwable> { call, cause ->

            call.application.environment.log.error(
                "Unhandled server error",
                cause
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                errorResponse("Đã có lỗi xảy ra từ phía máy chủ")
            )
        }
    }
}