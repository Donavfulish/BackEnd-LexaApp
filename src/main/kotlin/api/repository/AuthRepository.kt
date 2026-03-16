package api.repository

import api.models.dto.CourseDto
import api.models.dto.SignupRequest
import api.models.dto.UserInfo
import api.models.tables.RefreshTokensTable
import api.models.tables.UsersTable
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
import org.jetbrains.exposed.sql.selectAll

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
}