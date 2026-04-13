
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
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

val redirects = ConcurrentHashMap<String, String>()

@Serializable
data class UserSession(val state: String)

fun Application.configureSecurity(httpClient: HttpClient) {
    install(Sessions) {
        cookie<UserSession>("OAUTH_STATE") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.extensions["SameSite"] = "Lax" // Quan trọng để trình duyệt/webview chấp nhận cookie
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
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
            challenge { defaultScheme, realm ->
                // Thêm Header WWW-Authenticate: Bearer
                call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer realm=\"$realm\"")

                call.respondText(
                    text = "Token expired or invalid",
                    status = HttpStatusCode.Unauthorized
                )
            }
        }
        jwt("auth-jwt-oauth") {
            verifier(JwtConfig.verifier)
            validate { credential ->
                val type = credential.payload.getClaim("type").asString()
                if (credential.payload.subject != null && type == "oauth-access") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                // Thêm Header WWW-Authenticate: Bearer
                call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer realm=\"$realm\"")

                call.respondText(
                    text = "Token expired or invalid",
                    status = HttpStatusCode.Unauthorized
                )
            }
        }
        oauth("oauth-google") {
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
                    defaultScopes = listOf(
                        "https://www.googleapis.com/auth/userinfo.profile",
                        "https://www.googleapis.com/auth/userinfo.email" // THÊM DÒNG NÀY
                    ),

                    extraAuthParameters = listOf("access_type" to "offline"),
                    onStateCreated = { call, state ->
                        val target = call.request.queryParameters["redirectUrl"] ?: "lexa://auth-success"
                        redirects[state] = target
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
