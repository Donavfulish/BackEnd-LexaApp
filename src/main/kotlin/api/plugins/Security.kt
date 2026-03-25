
package com.lexa.api.plugins

import api.config.JwtConfig
import api.config.dotenv
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.respondRedirect
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

val dotenv = dotenv()

val redirects = ConcurrentHashMap<String, String>()

fun Application.configureSecurity(httpClient: HttpClient) {
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
        oauth("auth-oauth-google") {
            // Cấu hình URL callback (phải khớp với Google Console)
            urlProvider = { "http://localhost:8081/api/auth/oauth/google-callback" }

            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = dotenv["GOOGLE_CLIENT_ID"],
                    clientSecret = dotenv["GOOGLE_CLIENT_SECRET"],
                    defaultScopes = listOf("https://www.googleapis.com/auth/userinfo.profile"),
                    extraAuthParameters = listOf("access_type" to "offline"),
                    onStateCreated = { call, state ->
                        // Lưu lại URL mà user muốn quay lại sau khi login thành công
                        call.request.queryParameters["redirectUrl"]?.let {
                            redirects[state] = it
                        }
                    }
                )
            }

            fallback = { cause ->
                val currentCall = this.authentication.call
                if (cause is OAuth2RedirectError) {
                    currentCall.respondRedirect("/login-after-fallback")
                } else {
                    currentCall.respondText(
                        text = cause.message ?: "Unknown error",
                        status = HttpStatusCode.Forbidden
                    )
                }
            }
            client = httpClient
        }
    }
}

val applicationHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}
