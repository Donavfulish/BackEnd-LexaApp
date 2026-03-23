package api.repository

import api.models.dto.ShortCourseDto
import api.models.dto.CreateCourseRequest
import api.models.dto.CreatorDto
import api.models.dto.ShortSpeakingDayDto
import api.models.dto.SpeakingCourseDetailDto
import api.models.tables.CoursesTable
import api.models.tables.UsersTable
import api.models.tables.TopicsTable
import api.models.tables.UserFavoriteCoursesTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import api.models.tables.SpeakingDaysTable
import api.models.enum.PrivacyType
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.*
import kotlin.collections.map

// Nơi giao tiếp trực tiếp với database (nơi này sẽ được sử dụng models và dto)
class CoursesRepository {
    suspend fun getAllCourses(userId: Int): List<ShortCourseDto> = dbQuery {

        CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
            .select { CoursesTable.privacy eq PrivacyType.PUBLIC }
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
    }
    suspend fun getSpeakingDayCourse(userId: Int, courseId: Long): SpeakingCourseDetailDto? = dbQuery {

        val row = CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
            .select { (CoursesTable.privacy eq PrivacyType.PUBLIC) and (CoursesTable.id eq courseId) }
            .singleOrNull() ?: return@dbQuery null

        val courseIdEntity = row[CoursesTable.id]
        val isFavorite = UserFavoriteCoursesTable
            .select {
                (UserFavoriteCoursesTable.courseId eq courseIdEntity) and
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
                    SpeakingDaysTable.courseId eq courseIdEntity
                }
                .firstOrNull()
                ?.get(learnerCountExpr) ?: 0L
                    ).toInt()
        }

        val favoriteCountExpr = (UserFavoriteCoursesTable.userId.count())
        val favoriteUserCount = (UserFavoriteCoursesTable
            .slice(favoriteCountExpr)
            .select { UserFavoriteCoursesTable.courseId eq courseIdEntity }
            .firstOrNull()
            ?.get(favoriteCountExpr) ?: 0L).toInt()

        val list_speaking_day = SpeakingDaysTable
            .select { SpeakingDaysTable.courseId eq courseIdEntity }
            .orderBy(SpeakingDaysTable.dayOrder to SortOrder.ASC)
            .map { dayRow ->
                val dayId = dayRow[SpeakingDaysTable.id]

                val totalParaExpr = SpeakingParagraphsTable.id.count()
                val totalParas: Long = SpeakingParagraphsTable
                    .slice(totalParaExpr)
                    .select { SpeakingParagraphsTable.speakingDayId eq dayId }
                    .firstOrNull()
                    ?.get(totalParaExpr) ?: 0L

                val doneParaExpr = SpeakingParagraphResultsTable.paragraphId.count()
                val doneParas: Long = (SpeakingParagraphResultsTable
                    .innerJoin(SpeakingParagraphsTable)
                    .slice(doneParaExpr)
                    .select {
                        (SpeakingParagraphsTable.speakingDayId eq dayId) and
                                (SpeakingParagraphResultsTable.userId eq userId)
                    }
                    .firstOrNull()
                    ?.get(doneParaExpr) ?: 0L)

                val completed = if (totalParas == 0L) 0 else ((doneParas * 100) / totalParas).toInt()

                ShortSpeakingDayDto(
                    title = dayRow[SpeakingDaysTable.title] ?: "",
                    completed = completed
                )
            }

        // Trả về DTO cuối cùng
        SpeakingCourseDetailDto(
            id = courseIdEntity.value,
            thumbnail_url = row[CoursesTable.thumbnailUrl],
            creator = CreatorDto(
                name = row[UsersTable.name],
                image = row[UsersTable.avatarUrl]
            ),
            type = row[TopicsTable.name],
            typeColor = row[TopicsTable.color],
            is_favorite = isFavorite,
            title = row[CoursesTable.title],
            studying_user_count = studyingUserCount,
            favorite_user_count = favoriteUserCount,
            description = row[CoursesTable.description],
            deckId = row[CoursesTable.deckId]!!.value,
            list_speaking_day = list_speaking_day
        )
    }
    suspend fun getFeaturedCourses(userId: Int): List<ShortCourseDto> = dbQuery {

        CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
            .select { CoursesTable.privacy eq PrivacyType.PUBLIC }
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
    suspend fun getTopStudiedCourses(userId: Int): List<ShortCourseDto> = dbQuery {

        CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
            .select { CoursesTable.privacy eq PrivacyType.PUBLIC }
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
            .sortedByDescending { it.studying_user_count }
    }

    suspend fun getStudyingCourses(userId: Int): List<ShortCourseDto> = dbQuery {

        CoursesTable
            .innerJoin(SpeakingDaysTable)
            .innerJoin(SpeakingParagraphsTable)
            .innerJoin(SpeakingParagraphResultsTable)
            .innerJoin(UsersTable, { CoursesTable.creatorId }, { UsersTable.id })
            .leftJoin(TopicsTable)
            .slice(CoursesTable.columns + UsersTable.columns + TopicsTable.columns)
            .select {
                (CoursesTable.privacy eq PrivacyType.PUBLIC) and
                        (SpeakingParagraphResultsTable.userId eq userId)
            }
            .withDistinct()
            .map { row ->

                val courseId = row[CoursesTable.id]

                // ===== isFavorite =====
                val isFavorite = UserFavoriteCoursesTable
                    .select {
                        (UserFavoriteCoursesTable.courseId eq courseId) and
                                (UserFavoriteCoursesTable.userId eq userId)
                    }
                    .empty().not()

                // ===== studying user count =====
                val studyingUserCount: Int = run {
                    val learnerCountExpr = SpeakingParagraphResultsTable.userId.countDistinct()

                    (SpeakingParagraphResultsTable
                        .innerJoin(SpeakingParagraphsTable)
                        .innerJoin(SpeakingDaysTable)
                        .slice(learnerCountExpr)
                        .select { SpeakingDaysTable.courseId eq courseId }
                        .firstOrNull()
                        ?.get(learnerCountExpr) ?: 0L
                            ).toInt()
                }

                // ===== favorite user count =====
                val favoriteCountExpr = UserFavoriteCoursesTable.userId.count()

                val favoriteUserCount = (UserFavoriteCoursesTable
                    .slice(favoriteCountExpr)
                    .select { UserFavoriteCoursesTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(favoriteCountExpr) ?: 0L).toInt()

                // ===== progress =====
                val totalDayExpr = SpeakingDaysTable.id.count()

                val totalDays: Long = SpeakingDaysTable
                    .slice(totalDayExpr)
                    .select { SpeakingDaysTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(totalDayExpr) ?: 0L

                val completedDays = SpeakingDaysTable
                    .select { SpeakingDaysTable.courseId eq courseId }
                    .count { dayRow ->
                        val dayId = dayRow[SpeakingDaysTable.id]

                        val totalParaExpr = SpeakingParagraphsTable.id.count()
                        val doneParaExpr = SpeakingParagraphResultsTable.paragraphId.count()

                        val totalParas: Long = SpeakingParagraphsTable
                            .slice(totalParaExpr)
                            .select { SpeakingParagraphsTable.speakingDayId eq dayId }
                            .first()[totalParaExpr]

                        val doneParas: Long =
                            (SpeakingParagraphResultsTable
                                .innerJoin(SpeakingParagraphsTable)
                                .slice(doneParaExpr)
                                .select {
                                    (SpeakingParagraphsTable.speakingDayId eq dayId) and
                                            (SpeakingParagraphResultsTable.userId eq userId)
                                }
                                .firstOrNull()
                                ?.get(doneParaExpr) ?: 0L)

                        doneParas == totalParas && totalParas > 0
                    }

                val completed =
                    if (totalDays == 0L) 0
                    else ((completedDays * 100) / totalDays).toInt()

                ShortCourseDto(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    type = row[TopicsTable.name],
                    title = row[CoursesTable.title],
                    creator_name = row[UsersTable.name],
                    is_favorite = isFavorite,
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount,
                    completed = completed
                )
            }
            .sortedByDescending { it.studying_user_count }
    }

    suspend fun getMyCourses(userId: Int): List<ShortCourseDto> = dbQuery {

        CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
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


    suspend fun createCourse(request: CreateCourseRequest): Long = dbQuery {
        CoursesTable.insertAndGetId {
            it[topicId] = request.topicId
            it[title] = request.title
            it[description] = request.description
            it[creatorId] = request.creatorId
            it[privacy] = api.models.enum.PrivacyType.valueOf(request.privacy)
        }.value
    }

    suspend fun getFavoriteCourses(userId: Int): List<ShortCourseDto> = dbQuery {

        CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
            .innerJoin(UserFavoriteCoursesTable)
            .select {
                (CoursesTable.privacy eq PrivacyType.PUBLIC) and
                        (UserFavoriteCoursesTable.userId eq userId)
            }
            .map { row ->
                val courseId = row[CoursesTable.id]
                val isFavorite = true
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
    }
}