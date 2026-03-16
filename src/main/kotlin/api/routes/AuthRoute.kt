package api.routes

import api.models.dto.CreateCourseRequest
import api.models.dto.LoginRequest
import api.models.dto.RefreshRequest
import api.models.dto.SignupRequest
import api.models.dto.successResponse
import api.services.AuthService
import com.lexa.api.services.CoursesService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

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
                successResponse(result, "Đăng nhập thành công")
            )
        }
    }
    route("api/auth/signup") {
        post {
            /*
            *   {
                  "name": "Huỳnh Gia Bịp",
                  "email": "hgau23@clc.fitus.edu.vn",
                  "role": "TEACHER",
                  "password": "12345678"
                }
            * */
            val signupRequest = call.receive<SignupRequest>()

            val result = authService.signup(signupRequest)

            call.respond(
                HttpStatusCode.OK,
                successResponse(result, "Đăng ký thành công")
            )
        }
    }

    route("api/auth/check-auth") {
        authenticate("auth-jwt") { // Bọc phương thức trong hàm này để xác thực token
            get {
                call.respond(
                    HttpStatusCode.OK,
                    successResponse("Bạn có quyền truy cập", "Bạn đã đăng nhập")
                )
            }
        }
    }

    route("/api/auth/refresh") {
        post {
            val refreshToken = call.receive<RefreshRequest>()

            val result = authService.refreshAccessToken(refreshToken)

            if (result.ok) {
                call.respond(HttpStatusCode.OK, result)
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

    route("/api/auth/google") {
        get("/check") {
            // TODO Trả về accessToken + refreshToken nếu tk google này đã tồn tại
            // TODO Nếu tk chưa tồn tại, thì trả về name + ngày sinh... detect được
        }
        get("/signup") {
            // TODO Xử lý đăng ký, trả về accessToken + refreshToken
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
}