package api.repository

import api.models.dto.ShortCourseDto
import api.models.enum.PrivacyType
import api.models.tables.CoursesTable
import api.models.tables.SpeakingDaysTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import api.models.tables.TopicsTable
import api.models.tables.UserFavoriteCoursesTable
import api.models.tables.UsersTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.select

class SpeakingDayRepository {
    suspend fun getParagraphSpeakingDay(userId: Int): List<ShortCourseDto> = dbQuery {

        SpeakingDaysTable
            .select { (CoursesTable.privacy eq PrivacyType.PUBLIC) and (CoursesTable.creatorId eq userId) }
            .map { row ->

                val courseId = row[CoursesTable.id]
                val deckId = row[CoursesTable.deckId]

                val isFavorite = UserFavoriteCoursesTable
                    .select {
                        (UserFavoriteCoursesTable.courseId eq courseId) and
                                (UserFavoriteCoursesTable.userId eq userId)
                    }
                    .empty().not()

                val studyingUserCount: Int = run {
                    val learnerCountExpr = SpeakingParagraphResultsTable.userId.countDistinct()

                    (SpeakingParagraphResultsTable
                        .innerJoin(SpeakingParagraphsTable)
                        .innerJoin(SpeakingDaysTable)
                        .slice(learnerCountExpr)
                        .select {
                            SpeakingDaysTable.courseId eq courseId
                        }
                        .firstOrNull()
                        ?.get(learnerCountExpr) ?: 0L
                            ).toInt()
                }

                val favoriteCountExpr = (UserFavoriteCoursesTable.userId.count())

                val favoriteUserCount = (UserFavoriteCoursesTable
                    .slice(favoriteCountExpr)
                    .select { UserFavoriteCoursesTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(favoriteCountExpr) ?: 0L).toInt()

                ShortCourseDto(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    type = row[TopicsTable.name],
                    title = row[CoursesTable.title],
                    creator_name = row[UsersTable.name],
                    is_favorite = isFavorite,
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount
                )
            }
            .sortedByDescending { it.favorite_user_count }
    }
}