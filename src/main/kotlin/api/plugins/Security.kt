
package com.lexa.api.plugins

import api.config.JwtConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") { // Đây chính là "middleware"
            verifier(JwtConfig.verifier)
            validate { credential ->
                val type = credential.payload.getClaim("type").asString()
                // Chỉ chấp nhận token loại "access" cho các route thông thường
                if (credential.payload.subject != null && type == "access") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respondText(
                    text = "Token expired or invalid",
                    status = HttpStatusCode.Unauthorized
                )
            }
        }
    }
}
