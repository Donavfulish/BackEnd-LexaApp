package api.repository

import api.models.dto.OAuthRegisterRequest
import api.models.dto.SignupRequest
import api.models.dto.UserInfo
import api.models.enum.OtpPurpose
import api.models.enum.ProviderType
import api.models.tables.AuthProviderTable
import api.models.tables.RefreshTokensTable
import api.models.tables.UserOtpsTable
import api.models.tables.UsersTable
import api.utils.AuthUtil
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime
import kotlin.time.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate

class AuthRepository {
    suspend fun findById(userId: Int): UserInfo? = dbQuery {
        UsersTable.select { UsersTable.id eq userId }
            .map {
                    row ->
                UserInfo(
                    id = row[UsersTable.id].value,
                    name = row[UsersTable.name],
                    email = row[UsersTable.email],
                    passwordHash = row[UsersTable.passwordHash],
                    role = row[UsersTable.role]
                )
            }
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): UserInfo? = dbQuery {
        UsersTable.select { UsersTable.email eq email }
            .map {
                row ->
                UserInfo(
                    id = row[UsersTable.id].value,
                    name = row[UsersTable.name],
                    email = row[UsersTable.email],
                    passwordHash = row[UsersTable.passwordHash],
                    role = row[UsersTable.role]
                )
            }
            .singleOrNull()
    }

    suspend fun existsByEmail(email: String): Boolean = dbQuery {
        !UsersTable
            .select { UsersTable.email eq email }
            .empty()
    }

    suspend fun createUser(signupRequest: SignupRequest, hashedPassword: String): UserInfo = dbQuery {
        val result = UsersTable.insert {
            it[email] = signupRequest.email
            it[passwordHash] = hashedPassword
            it[name] = signupRequest.name
            it[role] = signupRequest.role
            it[languageCertificate] = signupRequest.english_certificate_url
            it[teachingDegree] = signupRequest.pedagogical_certificate_url
            it[dateOfBirth] = LocalDate.parse(signupRequest.date_of_birth)
            it[emailVerified] = true
        }

        val row = result.resultedValues!!.first()

        UserInfo(
            id = row[UsersTable.id].value,
            email = row[UsersTable.email],
            name = row[UsersTable.name],
            role = row[UsersTable.role],
        )
    }

    suspend fun storeRefreshToken(_userId: Int, _refreshToken: String) = dbQuery {
        // Xóa token cũ của user này (nếu muốn mỗi user chỉ có 1 phiên đăng nhập)
        RefreshTokensTable.deleteWhere { userId eq _userId }

        // Chèn token mới
        RefreshTokensTable.insert {
            it[userId] = _userId
            it[tokenHash] = _refreshToken
            it[expiresAt] = LocalDateTime.now().plusDays(30)
        }
    }

    suspend fun validateRefreshToken(userIdParam: Int, token: String): Boolean = dbQuery {
        // Tìm dòng có userId và token khớp hoàn toàn
        val exists = RefreshTokensTable
            .select {
                (RefreshTokensTable.userId eq userIdParam) and (RefreshTokensTable.tokenHash eq token)
            }
            .singleOrNull()

        if (exists == null) return@dbQuery false

        // Kiểm tra thời hạn (expiry)
        val expiry = exists[RefreshTokensTable.expiresAt]
        expiry.isAfter(LocalDateTime.now())
    }

    suspend fun createOTP(_email: String, _otpCode: String) = dbQuery {
        UserOtpsTable.insert {
            it[email] = _email
            it[otpCode] = _otpCode
            it[purpose] = OtpPurpose.VERIFY_EMAIL
            it[isUsed] = false
            it[expiresAt] = LocalDateTime.now().plusMinutes(2)
        }
    }

    suspend fun verifyOTP(_email: String, _otpCode: String): Boolean = dbQuery {
        val now = LocalDateTime.now()

        val updateCount = UserOtpsTable.update({
            (UserOtpsTable.email eq _email) and
            (UserOtpsTable.otpCode eq _otpCode) and
            (UserOtpsTable.isUsed eq false) and
            (UserOtpsTable.expiresAt greater now)
        }) {
            it[isUsed] = true
        }

        updateCount > 0
    }

    suspend fun createOAuthUser(request: OAuthRegisterRequest, oauthSub: String): UserInfo = dbQuery {
        transaction {
            val result = UsersTable.insert {
                it[name] = request.name
                it[email] = request.email
                it[emailVerified] = true
                it[address] = request.address
                it[role] = request.role
                it[languageCertificate] = request.english_certificate_url
                it[teachingDegree] = request.pedagogical_certificate_url
            }

            val userId = result[UsersTable.id]

            AuthProviderTable.insert {
                it[AuthProviderTable.userId] = userId
                it[AuthProviderTable.provider] = request.provider
                it[AuthProviderTable.providerUserId] = oauthSub
            }

            val row = result.resultedValues!!.first()

            UserInfo(
                id = row[UsersTable.id].value,
                email = row[UsersTable.email],
                name = row[UsersTable.name],
                role = row[UsersTable.role],
            )
        }
    }

    suspend fun getUserFromOAuth(sub: String, provider: ProviderType): UserInfo? = dbQuery {
        AuthProviderTable
            .innerJoin(UsersTable)
            .select {
                (AuthProviderTable.provider eq provider) and
                        (AuthProviderTable.providerUserId eq sub)
            }
            .map { row ->
                UserInfo(
                    id = row[UsersTable.id].value,
                    email = row[UsersTable.email],
                    name = row[UsersTable.name],
                    role = row[UsersTable.role],
                )
            }
            .singleOrNull()
    }
}