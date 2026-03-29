package api.utils

import api.config.getUserId
import api.models.dto.errorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond


suspend fun ApplicationCall.getUserIdOrRespond(): Int? {
    val userId = getUserId()
    if (userId == null) {
        respond(HttpStatusCode.Unauthorized, errorResponse("Unauthorized"))
    }
    return userId
}

suspend fun ApplicationCall.getLongParamOrRespond(param: String): Long? {
    val value = parameters[param]?.toLongOrNull()
    if (value == null) {
        respond(HttpStatusCode.BadRequest, errorResponse("Invalid $param"))
    }
    return value
}