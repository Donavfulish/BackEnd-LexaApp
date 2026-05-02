package api.repository

import api.models.dto.ChangePasswordRequest
import api.models.dto.GetAchievementResponse
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import api.models.dto.GetProfileResponse
import api.models.dto.UpdateFcmTokenRequest
import api.models.dto.UpdateProfileRequest
import api.models.dto.UserInfo
import api.models.tables.FlashcardDecksTable
import api.models.tables.FlashcardResultsTable
import api.models.tables.SpeakingDaysTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
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
        val userRow = UsersTable
            .select { UsersTable.id eq userId }
            .singleOrNull() ?: throw Exception("User not found")

        // 2. activeCourses: Đếm số khóa học user đã tham gia (có kết quả trong SpeakingParagraphResults)
        // Cần join: SpeakingParagraphResults -> SpeakingParagraphs -> SpeakingDays -> Courses
        val activeCoursesCount = SpeakingParagraphResultsTable
            .innerJoin(SpeakingParagraphsTable)
            .innerJoin(SpeakingDaysTable)
            .slice(SpeakingDaysTable.courseId)
            .select { SpeakingParagraphResultsTable.userId eq userId }
            .withDistinct() // Đảm bảo một khóa học không bị đếm nhiều lần
            .count()

        // 3. vocabularies: Đếm số từ vựng trong FlashcardResultsTable
        val vocabCount = FlashcardResultsTable
            .select { FlashcardResultsTable.userId eq userId }
            .count()

        // 4. vocabSets: Đếm số lượng bộ thẻ do user sở hữu (creatorId)
        val vocabSetsCount = FlashcardDecksTable
            .select { FlashcardDecksTable.creatorId eq userId }
            .count()


        val exposedLocalDate = userRow[UsersTable.dateOfBirth]
        val utilDate = exposedLocalDate?.let { java.sql.Date.valueOf(it) }

        GetProfileResponse(
            id = userRow[UsersTable.id].value,
            fullName = userRow[UsersTable.name],
            DoB = utilDate ?: java.sql.Date.valueOf(userRow[UsersTable.dateOfBirth]),
            address = userRow[UsersTable.address],
            avatarUrl = userRow[UsersTable.avatarUrl],
            email = userRow[UsersTable.email],
            activeCourses = activeCoursesCount.toInt(),
            vocabularies = vocabCount.toInt(),
            vocabSets = vocabSetsCount.toInt()
        )
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
    suspend fun getAchievements(teacherId: Int): GetAchievementResponse = dbQuery {

        val studentCount = api.models.tables.CoursesTable
            .innerJoin(SpeakingDaysTable, onColumn = { api.models.tables.CoursesTable.id }, otherColumn = { SpeakingDaysTable.courseId })
            .innerJoin(SpeakingParagraphsTable, onColumn = { SpeakingDaysTable.id }, otherColumn = { SpeakingParagraphsTable.speakingDayId })
            .innerJoin(SpeakingParagraphResultsTable, onColumn = { SpeakingParagraphsTable.id }, otherColumn = { SpeakingParagraphResultsTable.paragraphId })
            .slice(SpeakingParagraphResultsTable.userId)
            .select { api.models.tables.CoursesTable.creatorId eq teacherId }
            .withDistinct()
            .count()


        val favoriteCount = api.models.tables.CoursesTable
            .innerJoin(UserFavoriteCoursesTable, onColumn = { api.models.tables.CoursesTable.id }, otherColumn = { UserFavoriteCoursesTable.courseId })
            .select { api.models.tables.CoursesTable.creatorId eq teacherId }
            .count()

        GetAchievementResponse(
            countStudent = studentCount.toInt(),
            countFavorite = favoriteCount.toInt()
        )
    }
}
