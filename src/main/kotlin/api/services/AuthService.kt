package api.services

import api.config.JwtConfig
import api.config.MailFactory
import api.models.dto.AuthResult
import api.models.dto.LoginRequest
import api.models.dto.OAuthRegisterRequest
import api.models.dto.RefreshRequest
import api.models.dto.SignupRequest
import api.models.dto.UserInfo
import api.models.dto.UserResponse
import api.models.enum.ProviderType
import api.models.tables.UsersTable
import api.repository.AuthRepository
import api.repository.CoursesRepository
import api.utils.AuthUtil
import api.utils.AuthUtil.toResponse
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.ResultRow
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import io.github.cdimascio.dotenv.dotenv
import java.util.Properties

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

        return AuthResult(true,"Đăng nhập thành công", user.id, user.toResponse(), accessToken, refreshToken)
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

        return AuthResult(true, "Đăng ký thành công", user.id, user.toResponse(), accessToken, refreshToken)
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

    suspend fun sendOtpEmail(recipientEmail: String, otpCode: String) {
        val subject = "Mã xác thực OTP của bạn"

        // Tạo nội dung HTML để email trông đẹp hơn
        val htmlContent = """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px;">
            <h2 style="color: #4A90E2; text-align: center;">Xác thực tài khoản</h2>
            <p>Chào bạn,</p>
            <p>Bạn vừa yêu cầu mã OTP để đăng nhập hoặc xác thực tài khoản. Mã của bạn là:</p>
            <div style="text-align: center; margin: 30px 0;">
                <span style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #333; background: #f4f4f4; padding: 10px 20px; border-radius: 5px;">
                    $otpCode
                </span>
            </div>
            <p style="color: #666; font-size: 14px;">Mã này có hiệu lực trong vòng <b>2 phút</b>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
            <p style="font-size: 12px; color: #999; text-align: center;">Đây là email tự động, vui lòng không phản hồi.</p>
        </div>
    """.trimIndent()

        authRepository.createOTP(recipientEmail, otpCode)

        MailFactory.sendEmail(
            to = recipientEmail,
            subject = subject,
            body = htmlContent
        )
    }

    suspend fun verifyOtpEmail(recipientEmail: String, otpCode: String): Boolean {
        return authRepository.verifyOTP(recipientEmail, otpCode)
    }

    suspend fun isOAuthUserExisted(sub: String, provider: ProviderType): Boolean {
        return authRepository.isOAuthUserExisted(sub, provider)
    }

    suspend fun checkOAuth(sub: String, provider: ProviderType): AuthResult {
        val user = authRepository.getUserFromOAuth(sub, provider)

        if (user == null) return AuthResult(
            ok = false,
            message = "Tài khoản chưa được đăng ký"
        )

        val accessToken = JwtConfig.generateAccessToken(user)
        val refreshToken = JwtConfig.generateRefreshToken(user)

        authRepository.storeRefreshToken(user.id, refreshToken)

        return AuthResult(
            ok = true,
            message = "Đăng nhập thành công bằng ${provider.toString()}",
            id = user.id,
            user = user.toResponse(),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    suspend fun signupOAuth(registerRequest: OAuthRegisterRequest, oauthSub: String): AuthResult {
        println("sub: $oauthSub, provider: ${registerRequest.provider.toString()}")
        val existedUser = authRepository.getUserFromOAuth(oauthSub, registerRequest.provider)

        if (existedUser != null) return AuthResult(
            ok = false,
            message = "Tài khoản đã tồn tại"
        )

        val user = authRepository.createOAuthUser(registerRequest, oauthSub)

        val accessToken = JwtConfig.generateAccessToken(user)
        val refreshToken = JwtConfig.generateRefreshToken(user)

        authRepository.storeRefreshToken(user.id, refreshToken)

        return AuthResult(
            ok = true,
            message = "Đăng ký thành công bằng ${registerRequest.provider.toString()}",
            id = user.id,
            user = user.toResponse(),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}