package api.repository

import api.models.dto.ShortCourseDto
import api.models.dto.CreateCourseRequest
import api.models.dto.CreatorDto
import api.models.dto.GetFeaturedCourseResponse
import api.models.dto.GetStudyingCourseResponse
import api.models.dto.ShortSpeakingDayDto
import api.models.dto.SpeakingCourseDetailDto
import api.models.dto.TopicDto
import api.models.tables.CoursesTable
import api.models.tables.UsersTable
import api.models.tables.TopicsTable
import api.models.tables.UserFavoriteCoursesTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import api.models.tables.SpeakingDaysTable
import api.models.enum.PrivacyType
import api.models.tables.FlashcardsTable
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

                val vocabNumber = row[CoursesTable.deckId]?.let { dId ->
                    FlashcardsTable.select { FlashcardsTable.deckId eq dId }.count()
                } ?: 0L

                val favoriteCountExpr = (UserFavoriteCoursesTable.userId.count())

                val favoriteUserCount = (UserFavoriteCoursesTable
                    .slice(favoriteCountExpr)
                    .select { UserFavoriteCoursesTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(favoriteCountExpr) ?: 0L).toInt()

                ShortCourseDto(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    topic = TopicDto(
                        id = row[TopicsTable.id]?.value ?: 0,
                        name = row[TopicsTable.name] ?: "",
                        colorHex = row[TopicsTable.color] ?: "#636AE8"),
                    title = row[CoursesTable.title],
                    description = row[CoursesTable.description] ?: "",
                    creator_name = row[UsersTable.name],
                    creator_avatar_url = row[UsersTable.avatarUrl] ?: "",
                    is_favorite = isFavorite,
                    vocabNumber = vocabNumber.toInt(),
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount
                )
            }
    }
    suspend fun getSpeakingDayCourses(userId: Int, courseId: Long): List<SpeakingCourseDetailDto> = dbQuery {

        CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
            .select { (CoursesTable.privacy eq PrivacyType.PUBLIC) and (CoursesTable.id eq courseId)}
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

                val list_speaking_day = SpeakingDaysTable
                    .select { SpeakingDaysTable.courseId eq courseId }
                    .orderBy(SpeakingDaysTable.dayOrder to SortOrder.ASC)
                    .map { dayRow ->

                        val dayId = dayRow[SpeakingDaysTable.id]

                        // ===== Tổng paragraph =====
                        val totalParaExpr = SpeakingParagraphsTable.id.count()

                        val totalParas: Long = SpeakingParagraphsTable
                            .slice(totalParaExpr)
                            .select { SpeakingParagraphsTable.speakingDayId eq dayId }
                            .firstOrNull()
                            ?.get(totalParaExpr) ?: 0L

                        // ===== Paragraph đã làm =====
                        val doneParaExpr = SpeakingParagraphResultsTable.paragraphId.count()

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

                        // ===== % completed =====
                        val completed =
                            if (totalParas == 0L) 0
                            else ((doneParas * 100) / totalParas).toInt()

                        ShortSpeakingDayDto(
                            title = dayRow[SpeakingDaysTable.title] ?: "",
                            completed = completed
                        )
                    }


                SpeakingCourseDetailDto(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    creator = CreatorDto(
                        name = row[UsersTable.name],
                        image = row[UsersTable.avatarUrl]   // nếu có cột này
                    ),
                    type = row[TopicsTable.name],
                    is_favorite = isFavorite,
                    title = row[CoursesTable.title],
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount,
                    description = row[CoursesTable.description],
                    list_speaking_day = list_speaking_day
                )
            }
    }
    suspend fun getFeaturedCourses(userId: Int): List<GetFeaturedCourseResponse> = dbQuery {

        CoursesTable
            .innerJoin(UsersTable)
            .leftJoin(TopicsTable)
            .select { CoursesTable.privacy eq PrivacyType.PUBLIC }
            .map { row ->
                val courseId = row[CoursesTable.id]
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

                GetFeaturedCourseResponse(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    topic = TopicDto(
                        id = row[TopicsTable.id]?.value ?: 0,
                        name = row[TopicsTable.name] ?: "",
                        colorHex = row[TopicsTable.color] ?: "#636AE8"),
                    title = row[CoursesTable.title],
                    creator_name = row[UsersTable.name],
                    creator_avatar_url = row[UsersTable.avatarUrl] ?: "",
                    is_favorite = isFavorite,
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount
                )
            }
            .sortedByDescending { it.favorite_user_count }
    }
    suspend fun getTopStudiedCourses(userId: Int): List<GetFeaturedCourseResponse> = dbQuery {

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

                val vocabNumber = row[CoursesTable.deckId]?.let { dId ->
                    FlashcardsTable.select { FlashcardsTable.deckId eq dId }.count()
                } ?: 0L

                val favoriteCountExpr = (UserFavoriteCoursesTable.userId.count())

                val favoriteUserCount = (UserFavoriteCoursesTable
                    .slice(favoriteCountExpr)
                    .select { UserFavoriteCoursesTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(favoriteCountExpr) ?: 0L).toInt()

                GetFeaturedCourseResponse(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    topic = TopicDto(
                        id = row[TopicsTable.id]?.value ?: 0,
                        name = row[TopicsTable.name] ?: "",
                        colorHex = row[TopicsTable.color] ?: "#636AE8"),
                    title = row[CoursesTable.title],
                    creator_name = row[UsersTable.name],
                    creator_avatar_url = row[UsersTable.avatarUrl] ?: "",
                    is_favorite = isFavorite,
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount
                )
            }
            .sortedByDescending { it.studying_user_count }
    }

    suspend fun getStudyingCourses(userId: Int): List<GetStudyingCourseResponse> = dbQuery {

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



                GetStudyingCourseResponse(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    topic = TopicDto(
                        id = row[TopicsTable.id]?.value ?: 0,
                        name = row[TopicsTable.name] ?: "",
                        colorHex = row[TopicsTable.color] ?: "#636AE8"),
                    title = row[CoursesTable.title],
                    progress = completed)
            }
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

                val vocabNumber = row[CoursesTable.deckId]?.let { dId ->
                    FlashcardsTable.select { FlashcardsTable.deckId eq dId }.count()
                } ?: 0L

                val favoriteCountExpr = (UserFavoriteCoursesTable.userId.count())

                val favoriteUserCount = (UserFavoriteCoursesTable
                    .slice(favoriteCountExpr)
                    .select { UserFavoriteCoursesTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(favoriteCountExpr) ?: 0L).toInt()

                ShortCourseDto(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    topic = TopicDto(
                        id = row[TopicsTable.id]?.value ?: 0,
                        name = row[TopicsTable.name] ?: "",
                        colorHex = row[TopicsTable.color] ?: "#636AE8"),
                    title = row[CoursesTable.title],
                    description = row[CoursesTable.description] ?: "",
                    creator_name = row[UsersTable.name],
                    creator_avatar_url = row[UsersTable.avatarUrl] ?: "",
                    is_favorite = isFavorite,
                    vocabNumber = vocabNumber.toInt(),
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
                val vocabNumber = row[CoursesTable.deckId]?.let { dId ->
                    FlashcardsTable.select { FlashcardsTable.deckId eq dId }.count()
                } ?: 0L
                val favoriteCountExpr = (UserFavoriteCoursesTable.userId.count())
                val favoriteUserCount = (UserFavoriteCoursesTable
                    .slice(favoriteCountExpr)
                    .select { UserFavoriteCoursesTable.courseId eq courseId }
                    .firstOrNull()
                    ?.get(favoriteCountExpr) ?: 0L).toInt()

                ShortCourseDto(
                    id = courseId.value,
                    thumbnail_url = row[CoursesTable.thumbnailUrl],
                    topic = TopicDto(
                        id = row[TopicsTable.id]?.value ?: 0,
                        name = row[TopicsTable.name] ?: "",
                        colorHex = row[TopicsTable.color] ?: "#636AE8"),
                    title = row[CoursesTable.title],
                    description = row[CoursesTable.description] ?: "",
                    creator_name = row[UsersTable.name],
                    creator_avatar_url = row[UsersTable.avatarUrl] ?: "",
                    is_favorite = isFavorite,
                    vocabNumber = vocabNumber.toInt(),
                    studying_user_count = studyingUserCount,
                    favorite_user_count = favoriteUserCount
                )
            }
    }
}