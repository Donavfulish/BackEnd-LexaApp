package api.config

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
        .withClaim("id", userInfo.id)
        .withClaim("type", "access")
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