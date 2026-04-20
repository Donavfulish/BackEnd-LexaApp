package api.repository

import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import api.models.dto.GetProfileResponse
import api.models.dto.UpdateFcmTokenRequest
import api.models.dto.UpdateProfileRequest
import api.models.tables.SpeakingDaysTable
import api.models.tables.UserFavoriteCoursesTable
import api.models.tables.UsersTable
import api.services.CloudinaryService
import api.utils.toLocalDate
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.Date
import java.time.LocalDate
import java.time.LocalDateTime

class ProfileRepository {

    suspend fun getProfile(userId: Int): GetProfileResponse= dbQuery {
        UsersTable
            .slice(
                UsersTable.id,
                UsersTable.name,
                UsersTable.dateOfBirth,
                UsersTable.address,
                UsersTable.avatarUrl,
                UsersTable.email,
            )
            .select { UsersTable.id eq userId }
            .map {row ->

                val exposedLocalDate = row[UsersTable.dateOfBirth]
                val utilDate = exposedLocalDate?.let { java.sql.Date.valueOf(it) }

                GetProfileResponse(
                    id = row[UsersTable.id].value,
                    fullName = row[UsersTable.name],
                    DoB = utilDate ?: java.sql.Date.valueOf(row[UsersTable.dateOfBirth]),
                    address = row[UsersTable.address],
                    avatarUrl = row[UsersTable.avatarUrl],
                    email = row[UsersTable.email],
                )
            }
            .single()
    }

    suspend fun updateProfile(userId: Int, data: UpdateProfileRequest): Boolean = dbQuery {
        val updatedRows = UsersTable.update({ UsersTable.id eq userId }) {
            it[name] = data.fullName
            it[dateOfBirth] = data.DoB.toLocalDate()
            it[address] = data.address
        }

        updatedRows > 0
    }
    suspend fun updateFcmToken(userId: Int,  request: UpdateFcmTokenRequest): Boolean = dbQuery {
        val updatedRows = UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.fcmToken] = request.fcmToken
        }
        updatedRows > 0
    }
    suspend fun getFcmToken(userId: Int): String? = dbQuery {
        UsersTable
            .slice(UsersTable.fcmToken)
            .select { UsersTable.id eq userId }
            .map { row -> row[UsersTable.fcmToken] }
            .singleOrNull()
    }

    suspend fun getFcmTokensByUserIds(userIds: List<Int>): List<String> = dbQuery {
        UsersTable
            .slice(UsersTable.fcmToken)
            .select { UsersTable.id inList userIds }
            .mapNotNull { row -> row[UsersTable.fcmToken] }
    }
    suspend fun getUsersWhoFavoritedCourse(ownerId: Int,courseId: Long): List<Int> = dbQuery {
        UserFavoriteCoursesTable
            .slice(UserFavoriteCoursesTable.userId)
            .select {
                (UserFavoriteCoursesTable.courseId eq courseId) and
                        (UserFavoriteCoursesTable.userId neq ownerId)
            }
            .map { row ->
                row[UserFavoriteCoursesTable.userId].value
            }
    }

    suspend fun getUsersWhoFavoritedCourseBySpeakingDayId(ownerId: Int,speakingDayId: Long): List<Int> = dbQuery {

        UsersTable
            .innerJoin(UserFavoriteCoursesTable)
            .innerJoin(
                SpeakingDaysTable,
                onColumn = { UserFavoriteCoursesTable.courseId },
                otherColumn = { SpeakingDaysTable.courseId }
            )
            .slice(UsersTable.id)
            .select { (SpeakingDaysTable.id eq speakingDayId ) and
                    (UserFavoriteCoursesTable.userId neq ownerId)
            }
            .map { row ->
                row[UsersTable.id].value
            }
        }

    suspend fun updateAvatar(userId: Int, imageUrl: String?): Boolean = dbQuery {
        val oldAvatarUrl = UsersTable
            .slice(UsersTable.avatarUrl)
            .select { UsersTable.id eq userId }
            .singleOrNull()
            ?.get(UsersTable.avatarUrl)

        CloudinaryService.deleteImage(oldAvatarUrl ?: "")

        val updatedRows = UsersTable.update({ UsersTable.id eq userId }) {
            it[avatarUrl] = imageUrl
            it[updatedAt] = LocalDateTime.now()
        }

        updatedRows > 0
    }
}
