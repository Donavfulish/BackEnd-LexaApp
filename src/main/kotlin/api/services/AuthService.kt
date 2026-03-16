package api.services

import api.config.JwtConfig
import api.models.dto.AuthResult
import api.models.dto.LoginRequest
import api.models.dto.RefreshRequest
import api.models.dto.SignupRequest
import api.models.dto.UserInfo
import api.models.tables.UsersTable
import api.repository.AuthRepository
import api.repository.CoursesRepository
import api.utils.AuthUtil
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.ResultRow
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.github.cdimascio.dotenv.dotenv

val dotenv = dotenv()

class AuthService (
    private val authRepository: AuthRepository
) {
    suspend fun login(loginRequest: LoginRequest): AuthResult? {
        val email = loginRequest.email
        val password = loginRequest.password

        // 1. Tìm user theo email
        val user = authRepository.findByEmail(email)
            ?: return AuthResult(false, "Email không tồn tại")

        // 2. Kiểm tra mật khẩu
        val isPasswordCorrect = AuthUtil.verify(password, user.passwordHash!!)
        if (!isPasswordCorrect) {
            return AuthResult(false, "Email hoặc mật khẩu không chính xác")
        }

        // 3. Tạo cặp Token mới
        val accessToken = JwtConfig.generateAccessToken(user)
        val refreshToken = JwtConfig.generateRefreshToken(user)

        // 4. Cập nhật Refresh Token vào Database
        authRepository.storeRefreshToken(user.id, refreshToken)

        return AuthResult(true,"Đăng nhập thành công", accessToken, refreshToken)
    }

    suspend fun signup(signupRequest: SignupRequest): AuthResult {
        // 1. Kiểm tra email đã tồn tại chưa
        if (authRepository.existsByEmail(signupRequest.email)) {
            return AuthResult(false, "Email đã tồn tại")
        }

        // 2. Băm mật khẩu
        val hashedPassword = AuthUtil.hash(signupRequest.password)

        // 3. Lưu vào database
        val user = authRepository.createUser(signupRequest, hashedPassword)

        // 4. Lấy refreshToken + tạo accessToken
        val accessToken = JwtConfig.generateAccessToken(user)
        val refreshToken = JwtConfig.generateRefreshToken(user)

        // 5. Lưu refreshToken vào database
        authRepository.storeRefreshToken(user.id, refreshToken)

        return AuthResult(true, "Đăng ký thành công", accessToken, refreshToken)
    }

    suspend fun refreshAccessToken(refreshRequest: RefreshRequest): AuthResult  {
        val refreshToken = refreshRequest.refreshToken
        try {
            // 1. Giải mã token để lấy thông tin (chưa check hết hạn vì RefreshToken thường sống lâu)
            val decodedJWT = JwtConfig.verifier.verify(refreshToken)
            val userId = decodedJWT.getClaim("id").asInt()
            val type = decodedJWT.getClaim("type").asString()

            // 2. Kiểm tra xem có đúng là loại token "refresh" không
            if (type != "refresh") {
                return AuthResult(false, "Loại token không hợp lệ")
            }

            // 3. Kiểm tra tính hợp lệ trong Database
            val isValidInDb = authRepository.validateRefreshToken(userId, refreshToken)
            if (!isValidInDb) {
                return AuthResult(false, "Refresh Token không tồn tại hoặc đã hết hạn")
            }

            // 4. Tìm thông tin User để tạo AccessToken mới
            val user = authRepository.findById(userId) ?: return AuthResult(false, "User không tồn tại")

            // 5. Tạo AccessToken mới (giữ nguyên RefreshToken cũ hoặc tạo mới tùy bạn)
            val newAccessToken = JwtConfig.generateAccessToken(user)

            return AuthResult(
                ok = true,
                message = "Gia hạn thành công",
                accessToken = newAccessToken
            )
        } catch (e: Exception) {
            return AuthResult(false, "Token không hợp lệ hoặc đã bị chỉnh sửa")
        }
    }


}