package api.config

import api.models.dto.GoogleUserInfo
import api.models.dto.OAuthUserInfo
import api.models.dto.UserInfo
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.*

val dotenv = dotenv()

object JwtConfig {
    private const val validityInMs = 60_000 * 15 // 15 phút

    private val algorithm = Algorithm.HMAC256(dotenv["JWT_SECRET"])

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(dotenv["JWT_ISSUER"])
        .build()

    fun generateAccessToken(userInfo: UserInfo): String = JWT.create()
        .withSubject(userInfo.toString())
        .withIssuer(dotenv["JWT_ISSUER"])
        .withClaim("email", userInfo.email)
        .withClaim("id", userInfo.id)
        .withClaim("role", userInfo.role.toString())
        .withClaim("type", "access")
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)

    fun generateGoogleAccessToken(oauthUser: OAuthUserInfo): String = JWT.create()
        .withSubject(oauthUser.toString())
        .withIssuer(dotenv["JWT_ISSUER"])
        .withClaim("email", oauthUser.email)
        .withClaim("provider", oauthUser.provider.toString())
        .withClaim("sub", oauthUser.sub)
        .withClaim("type", "oauth-access")
        .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
        .sign(algorithm)

    // RefreshToken mình sẽ dùng cronjob để tự động reset trong Database
    fun generateRefreshToken(userInfo: UserInfo): String = JWT.create()
        .withSubject(userInfo.toString())
        .withIssuer(dotenv["JWT_ISSUER"])
        .withClaim("id", userInfo.id)
        .withClaim("type", "refresh")
        .sign(algorithm)
}

fun ApplicationCall.getUserId(): Int? {
    val principal = this.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("id")?.asInt()
}

fun ApplicationCall.getUserEmail(): String? {
    val principal = this.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("email")?.asString()
}

fun ApplicationCall.getUserRole(): String? {
    val principal = this.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("role")?.asString()
}


fun ApplicationCall.getOAuthProviderString(): String? {
    val principal = this.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("provider")?.asString()
}

fun ApplicationCall.getOAuthSub(): String? {
    val principal = this.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("sub")?.asString()
}