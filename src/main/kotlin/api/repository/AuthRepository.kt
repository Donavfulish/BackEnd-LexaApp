package api.repository

import api.models.dto.AuthResult
import api.models.dto.OAuthRegisterRequest
import api.models.dto.ResetPasswordRequest
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
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.rightJoin
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
                    role = row[UsersTable.role],
                    isEmailVerified = row[UsersTable.emailVerified]
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
                    role = row[UsersTable.role],
                    isEmailVerified = row[UsersTable.emailVerified]
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
            it[dateOfBirth] = signupRequest.date_of_birth
                ?.takeIf { it.isNotBlank() } // Chỉ đi tiếp nếu chuỗi không rỗng
                ?.let { LocalDate.parse(it) }
            it[emailVerified] = false
        }

        val row = result.resultedValues!!.first()

        UserInfo(
            id = row[UsersTable.id].value,
            email = row[UsersTable.email],
            name = row[UsersTable.name],
            role = row[UsersTable.role],
        )
    }

    suspend fun deleteRefreshToken(_userId: Int) = dbQuery {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq _userId }
    }

    suspend fun storeRefreshToken(_userId: Int, _refreshToken: String) = dbQuery {
        RefreshTokensTable.deleteWhere { userId eq _userId }

        RefreshTokensTable.insert {
            it[userId] = _userId
            it[tokenHash] = _refreshToken
            it[expiresAt] = LocalDateTime.now().plusDays(30)
        }
    }

    suspend fun validateRefreshToken(userIdParam: Int, token: String): Boolean = dbQuery {
        val exists = RefreshTokensTable
            .select {
                (RefreshTokensTable.userId eq userIdParam) and (RefreshTokensTable.tokenHash eq token)
            }
            .singleOrNull()

        if (exists == null) return@dbQuery false

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

        if (updateCount > 0) {
            UsersTable.update({
                UsersTable.email eq _email
            }) {
                it[emailVerified] = true
            }
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

    suspend fun getUserFromOAuth(sub: String, email: String?, provider: ProviderType): UserInfo? = dbQuery {
        var condition = (AuthProviderTable.provider eq provider) and (AuthProviderTable.providerUserId eq sub)

        if (!email.isNullOrBlank()) {
            condition = condition or (UsersTable.email eq email)
        }

        AuthProviderTable
            .rightJoin(UsersTable)
            .select(condition)
            .map { row ->
                UserInfo(
                    id = row[UsersTable.id].value,
                    email = row[UsersTable.email],
                    name = row[UsersTable.name],
                    role = row[UsersTable.role],
                    isEmailVerified = row[UsersTable.emailVerified]
                )
            }
            .singleOrNull()
    }

    suspend fun isOAuthUserExisted(sub: String, provider: ProviderType): Boolean = dbQuery {
        !AuthProviderTable
            .select {
            (AuthProviderTable.provider eq provider) and
                    (AuthProviderTable.providerUserId eq sub)
        }.empty()
    }

    suspend fun resetPassword(email: String, _passwordHash: String): Boolean = dbQuery {
        UsersTable.update({
            UsersTable.email eq email
        }) { row ->
            row[passwordHash] = _passwordHash
        } > 0
    }

    suspend fun updateEmail(oldEmail: String, newEmail: String): Boolean = dbQuery {
        val targetUserId = UsersTable
            .slice(UsersTable.id)
            .select { UsersTable.email eq oldEmail }
            .singleOrNull()
            ?.get(UsersTable.id)

        if (targetUserId == null) {
            return@dbQuery false
        }

        val isUpdated = UsersTable.update({ UsersTable.id eq targetUserId }) {
            it[email] = newEmail
        } > 0

        if (isUpdated) {
            AuthProviderTable.deleteWhere { userId eq targetUserId }
        }

        isUpdated
    }

    suspend fun updatePassword(userId: Int, _passwordHash: String): Boolean = dbQuery {
        UsersTable.update({UsersTable.id eq userId}) {
            it[passwordHash] = _passwordHash
        } > 0
    }
}