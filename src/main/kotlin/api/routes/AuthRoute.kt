package api.routes

import api.config.JwtConfig
import api.config.getOAuthSub
import api.config.getUserId
import api.config.getUserRole
import api.models.dto.CreateCourseRequest
import api.models.dto.ErrorResponse
import api.models.dto.GoogleUserInfo
import api.models.dto.LoginRequest
import api.models.dto.OAuthRegisterRequest
import api.models.dto.OAuthUserInfo
import api.models.dto.OtpRequest
import api.models.dto.OtpVerify
import api.models.dto.RefreshRequest
import api.models.dto.SignupRequest
import api.models.dto.successResponse
import api.models.enum.ProviderType
import api.services.AuthService
import com.lexa.api.plugins.applicationHttpClient
import com.lexa.api.plugins.redirects
import api.services.CoursesService
import api.utils.FileUtil
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.http.headers
import io.ktor.server.application.call
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.header
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.sessions
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

fun Route.authRoutes(authService: AuthService) {
    route("api/auth/login") {
        post {
            /*
            *   {
                  "email": "hgau23@clc.fitus.edu.vn",
                  "password": "12345678"
                }
            * */
            val loginRequest = call.receive<LoginRequest>()

            val result = authService.login(loginRequest)

            call.respond(
                HttpStatusCode.OK,
                successResponse(result)
            )
        }
    }
    route("api/auth/signup") {
        post {
            val multipart = call.receiveMultipart()
            var signupRequest: SignupRequest? = null
            var languageCertBytes: ByteArray? = null
            var pedagogyCertBytes: ByteArray? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "data") {
                            val jsonString = part.value
                            signupRequest = Json.decodeFromString<SignupRequest>(jsonString)
                        }
                    }
                    is PartData.FileItem -> {
                        val fileBytes = part.provider().readRemaining().readByteArray()
                        when (part.name) {
                            "languageCert" -> languageCertBytes = fileBytes
                            "pedagogyCert" -> pedagogyCertBytes = fileBytes
                        }
                        part.dispose()
                    }
                    else -> part.dispose()
                }
            }

            if (signupRequest == null) {
                call.respond(HttpStatusCode.BadRequest, "Thiếu dữ liệu đăng ký")
            }

            // Cập nhật đường dẫn file của payload thành đường dẫn được lưu trên Server
            languageCertBytes?.let { bytes ->
                val fileName = FileUtil.generateUniqueFileName("languageCert", "image.jpg")
                val savedPath = FileUtil.saveFileToDisk(bytes, fileName)
                signupRequest!!.english_certificate_url = savedPath
            }

            pedagogyCertBytes?.let { bytes ->
                val fileName = FileUtil.generateUniqueFileName("pedagogyCert", "image.jpg")
                val savedPath = FileUtil.saveFileToDisk(bytes, fileName)
                signupRequest!!.pedagogical_certificate_url = savedPath
            }

            val result = authService.signup(signupRequest!!)

            call.respond(
                HttpStatusCode.OK,
                successResponse(result, "Đăng ký thành công")
            )
        }
    }

    route("api/auth/check-auth") {
        authenticate("auth-jwt") { // Bọc phương thức trong hàm này để xác thực token
            get {
                val userId = call.getUserId()
                val userRole = call.getUserRole()
                call.respond(
                    HttpStatusCode.OK,
                    successResponse("id của bạn là $userId với role $userRole", "Bạn đã đăng nhập")
                )
            }
        }
    }

    route("/api/auth/refresh") {
        post {
            val request = call.receive<RefreshRequest>()


            println("refreshToken: ${request.refreshToken}")

            val result = authService.refreshAccessToken(request)

            if (result.ok) {
                call.respond(
                    HttpStatusCode.OK,
                    successResponse(result, result.message?: "Thành công"))
            } else {
                // Trả về 401 để Frontend biết đường đá người dùng ra màn Login
                call.respond(HttpStatusCode.Unauthorized, result)
            }
        }
    }

    route("/api/auth/logout") {

    }

    route("/api/auth/me") {

    }

    authenticate("oauth-google") {
        get("/api/auth/google/login") {
            // Ktor sẽ tự động redirect sang Google sau khi block này chạy xong
        }

        get("/api/auth/oauth/google-callback") {
            val params = call.request.queryParameters
            println("Callback params: ${params.entries()}") // Xem có 'code' và 'state' không

            val principal = call.principal<OAuthAccessTokenResponse.OAuth2>()

            if (principal == null) {
                call.respondRedirect("/login?error=failed")
                return@get
            }

            val token = principal.accessToken.trim()
            val response = applicationHttpClient.get("https://www.googleapis.com/oauth2/v3/userinfo") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }

            val jsonString = response.bodyAsText()
            println("Google Response JSON: $jsonString") // Xem nó có email/name thật không

            val googleUserInfo = response.body<GoogleUserInfo>()

            val oauthUserInfo = OAuthUserInfo(
                provider = ProviderType.GOOGLE,
                sub = googleUserInfo.sub,
                name = googleUserInfo.name,
                email = googleUserInfo.email,
                picture = googleUserInfo.picture
            )

            val isAccountRegistered = authService.isOAuthUserExisted(googleUserInfo.sub ?: "", ProviderType.GOOGLE)

            val googleAccessToken = JwtConfig.generateGoogleAccessToken(oauthUserInfo)

            println("Google Access Token: $googleAccessToken")

            val state = principal!!.state ?: ""
            val baseRedirect = redirects.remove(state) ?: "lexa://auth-success"

            // Lưu ý: encode URL các thành phần như name hoặc email để tránh lỗi ký tự đặc biệt
            val finalUrl = URLBuilder(baseRedirect).apply {
                parameters.append("token", googleAccessToken)
                parameters.append("email", oauthUserInfo.email)
                parameters.append("name", oauthUserInfo.name)
                parameters.append("registered", isAccountRegistered.toString())
                oauthUserInfo.picture?.let { parameters.append("avatar", it) }
            }.buildString()

            call.respondRedirect(finalUrl)
        }
    }


    route("/api/auth/google") {
        authenticate("auth-jwt-oauth") {
            get("/check") {
                val googleSub = call.getOAuthSub()

                if (googleSub == null) { call.respond(HttpStatusCode.Unauthorized, mapOf("sub" to googleSub)) }

                val result = authService.checkOAuth(googleSub!!, ProviderType.GOOGLE)

                if (result.ok) {
                    call.respond(HttpStatusCode.OK, successResponse(result, result.message ?: "Thành công"))
                } else {
                    // Trả về 401 để Frontend biết đường đá người dùng ra màn Login
                    call.respond(
                        HttpStatusCode.Unauthorized, successResponse(result, result.message?: "Thất bại")
                    )
                }
            }
            post("/signup") {
                val googleSub = call.getOAuthSub()

                if (googleSub == null) { call.respond(HttpStatusCode.Unauthorized, mapOf("sub" to googleSub)) }

                //val registerRequest = call.receive<OAuthRegisterRequest>()

                val multipart = call.receiveMultipart()
                var registerRequest: OAuthRegisterRequest? = null
                var languageCertBytes: ByteArray? = null
                var pedagogyCertBytes: ByteArray? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "data") {
                                val jsonString = part.value
                                registerRequest = Json.decodeFromString<OAuthRegisterRequest>(jsonString)
                            }
                        }
                        is PartData.FileItem -> {
                            val fileBytes = part.provider().readRemaining().readByteArray()
                            when (part.name) {
                                "languageCert" -> languageCertBytes = fileBytes
                                "pedagogyCert" -> pedagogyCertBytes = fileBytes
                            }
                            part.dispose()
                        }
                        else -> part.dispose()
                    }
                }

                if (registerRequest == null) {
                    call.respond(HttpStatusCode.BadRequest, "Thiếu dữ liệu đăng ký")
                }

                // Cập nhật đường dẫn file của payload thành đường dẫn được lưu trên Server
                languageCertBytes?.let { bytes ->
                    val fileName = FileUtil.generateUniqueFileName("languageCert", "image.jpg")
                    val savedPath = FileUtil.saveFileToDisk(bytes, fileName)
                    registerRequest!!.english_certificate_url = savedPath
                }

                pedagogyCertBytes?.let { bytes ->
                    val fileName = FileUtil.generateUniqueFileName("pedagogyCert", "image.jpg")
                    val savedPath = FileUtil.saveFileToDisk(bytes, fileName)
                    registerRequest!!.pedagogical_certificate_url = savedPath
                }

                val result = authService.signupOAuth(registerRequest!!, googleSub!!)

                if (result.ok) {
                    call.respond(
                        HttpStatusCode.OK,
                        successResponse(result,result.message?: "Thành công")
                    )
                } else {
                    // Trả về 401 để Frontend biết đường đá người dùng ra màn Login
                    call.respond(
                        HttpStatusCode.Unauthorized, successResponse(result, result.message?: "Thất bại")
                    )
                }
            }
        }
    }

    route("/api/auth/facebook") {
        get("/check") {
            // TODO Trả về accessToken + refreshToken nếu tk google này đã tồn tại
            // TODO Nếu tk chưa tồn tại, thì trả về name + ngày sinh... detect được
        }
        get("/signup") {
            // TODO Xử lý đăng ký, trả về accessToken + refreshToken
        }
    }

    route("/api/auth/otp") {
        post("/send") {
            val request = call.receive<OtpRequest>()
            val otpCode = (100000..999999).random().toString()

            try {
                // Cách 1: Chờ gửi xong mới trả về response (An toàn nhưng chậm hơn chút)
                authService.sendOtpEmail(request.email, otpCode)

                // Cách 2: Gửi ngầm và trả về response ngay (Nhanh, cần xử lý log lỗi riêng)
                // launch(Dispatchers.IO) { sendOtpEmail(request.email, otpCode) }

                call.respond(HttpStatusCode.OK, successResponse(null, "Mã OTP đã được gửi"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Không thể gửi mail: ${e.message}")
            }
        }

        post("/verify") {
            val request = call.receive<OtpVerify>()

            try {
                val verifyResult = authService.verifyOtpEmail(request.email, request.otp)

                if (verifyResult) {
                    call.respond(HttpStatusCode.OK, successResponse(null, "Mã OTP hợp lệ"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(false, "Mã OTP không đúng hoặc đã hết hạn"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Không thể gửi mail: ${e.message}")
            }
        }
    }
}