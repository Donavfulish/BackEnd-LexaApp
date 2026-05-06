package api.repository

import api.models.dto.AllCoursePaginationResponse
import api.models.dto.CourseDetailDto
import api.models.dto.ShortCourseDto
import api.models.dto.CreateCourseRequest
import api.models.dto.CreatorDto
import api.models.dto.EditCourseRequest
import api.models.dto.GetFeaturedCourseResponse
import api.models.dto.GetStudyingCourseResponse
import api.models.dto.SearchInfo
import api.models.dto.ShortSpeakingDayDto
import api.models.dto.TopicDto
import api.models.enum.OrderBy
import api.models.tables.CoursesTable
import api.models.tables.UsersTable
import api.models.tables.TopicsTable
import api.models.tables.UserFavoriteCoursesTable
import api.models.tables.SpeakingParagraphResultsTable
import api.models.tables.SpeakingParagraphsTable
import api.models.tables.SpeakingDaysTable
import api.models.enum.PrivacyType
import api.models.enum.SortBy
import api.models.tables.FlashcardsTable
import api.models.tables.SearchEngine
import api.services.CloudinaryService
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.collections.map
import org.jetbrains.exposed.sql.and
import java.time.LocalDateTime

// Nơi giao tiếp trực tiếp với database (nơi này sẽ được sử dụng models và dto)
class CoursesRepository {
    suspend fun getCourseSuggestions(query: String): List<String>{
        return SearchEngine.searchAllCourses(query).take(10).map { it.title }
    }
    private val speakingDayRepository = SpeakingDayRepository()
    private fun getStudyingUserCount(courseId: EntityID<Long>): Int {
        val learnerCountExpr = SpeakingParagraphResultsTable.userId.countDistinct()
        return (SpeakingParagraphResultsTable
            .innerJoin(SpeakingParagraphsTable)
            .innerJoin(SpeakingDaysTable)
            .slice(learnerCountExpr)
            .select { SpeakingDaysTable.courseId eq courseId }
            .firstOrNull()
            ?.get(learnerCountExpr) ?: 0L).toInt()
    }

    private fun getFavoriteUserCount(courseId: EntityID<Long>): Int {
        val favoriteCountExpr = UserFavoriteCoursesTable.userId.count()
        return (UserFavoriteCoursesTable
            .slice(favoriteCountExpr)
            .select { UserFavoriteCoursesTable.courseId eq courseId }
            .firstOrNull()
            ?.get(favoriteCountExpr) ?: 0L).toInt()
    }

    private fun isFavorite(courseId: EntityID<Long>, userId: Int): Boolean {
        return UserFavoriteCoursesTable
            .select { (UserFavoriteCoursesTable.courseId eq courseId) and (UserFavoriteCoursesTable.userId eq userId) }
            .empty().not()
    }

    private suspend fun executeCoursePagination(
        userId: Int,
        searchInfo: SearchInfo,
        nextCursor: Long?,
        baseQuery: Query,
        isOwner: Boolean = false,
        isFavoriteList: Boolean = false,
        isLearningList: Boolean = false
    ): AllCoursePaginationResponse {
        val query = searchInfo.query ?: ""
        val sortBy = SortBy.fromString(searchInfo.sortBy)
        val orderBy = OrderBy.fromString(searchInfo.order)
        val limit = searchInfo.limit ?: 10
        val lastId = nextCursor

        var isRevelant = false
        if (searchInfo.sortBy.isNullOrEmpty() and !searchInfo.query.isNullOrEmpty()) {
            isRevelant = true
        }

        var queryList = if (query.isNotEmpty()) {
            if (isOwner) {
                SearchEngine.searchMyCourses(query, userId)
            } else if (isFavoriteList) {
                SearchEngine.searchFavoriteCourses(query, userId)
            } else if (isLearningList){
                SearchEngine.searchLearningCourses(query, userId)
            } else {
                SearchEngine.searchAllCourses(query)
            }
        } else null

        if (queryList != null && queryList.isEmpty()) {
            return AllCoursePaginationResponse(emptyList(), searchInfo, null, 0)
        }

        val sortColumn = when (sortBy) {
            SortBy.CREATED -> CoursesTable.createdAt
            SortBy.TITLE -> CoursesTable.title
        }

        val lastValue = if (lastId != null && queryList.isNullOrEmpty()) {
            CoursesTable.slice(sortColumn).select { CoursesTable.id eq lastId }.singleOrNull()?.get(sortColumn)?.toString()
        } else null

        var totalCount = baseQuery.count()
        val sortOrder = if (orderBy == OrderBy.ASC) SortOrder.ASC else SortOrder.DESC

        if (!queryList.isNullOrEmpty()) {
            totalCount = queryList!!.size.toLong()
            if (lastId != null) {
                val startIndex = queryList!!.indexOfFirst { it.id == lastId }
                queryList = queryList!!.subList(startIndex + 1, if (startIndex + 10 > queryList!!.size) queryList!!.size else startIndex + 11)
            } else {
                queryList = queryList!!.subList(0, if (10 > queryList!!.size) queryList!!.size else 10)
            }
            baseQuery.andWhere { CoursesTable.id inList queryList!!.map { it.id } }
        }

        if (lastId != null && lastValue != null && !isRevelant) {
            baseQuery.andWhere {
                val lastTime = if (sortBy == SortBy.CREATED) LocalDateTime.parse(lastValue) else null
                if (sortOrder == SortOrder.DESC) {
                    when (sortBy) {
                        SortBy.CREATED -> (CoursesTable.createdAt less lastTime!!) or ((CoursesTable.createdAt eq lastTime) and (CoursesTable.id less lastId))
                        SortBy.TITLE -> (CoursesTable.title less lastValue) or ((CoursesTable.title eq lastValue) and (CoursesTable.id less lastId))
                    }
                } else {
                    when (sortBy) {
                        SortBy.CREATED -> (CoursesTable.createdAt greater lastTime!!) or ((CoursesTable.createdAt eq lastTime) and (CoursesTable.id greater lastId))
                        SortBy.TITLE -> (CoursesTable.title greater lastValue) or ((CoursesTable.title eq lastValue) and (CoursesTable.id greater lastId))
                    }
                }
            }
        }

        if (!isRevelant) baseQuery.orderBy(sortColumn, sortOrder).limit(limit)

        val deckIds = baseQuery.limit(limit).mapNotNull { it[CoursesTable.deckId] }
        val vocabCountsMap = FlashcardsTable
            .slice(FlashcardsTable.deckId, FlashcardsTable.id.count())
            .select { FlashcardsTable.deckId inList deckIds }
            .groupBy(FlashcardsTable.deckId)
            .associate { it[FlashcardsTable.deckId] to it[FlashcardsTable.id.count()] }


        val results = baseQuery.limit(limit).map { row ->
            val courseId = row[CoursesTable.id]
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
                topic = TopicDto(
                    id = row[TopicsTable.id]?.value ?: 0,
                    name = row[TopicsTable.name] ?: "",
                    colorHex = row[TopicsTable.color] ?: "#636AE8"
                ),
                title = row[CoursesTable.title],
                description = row[CoursesTable.description] ?: "",
                creator_name = row[UsersTable.name],
                creator_avatar_url = row[UsersTable.avatarUrl] ?: "",
                is_favorite = if (isFavoriteList) true else isFavorite(courseId, userId),
                vocabNumber = (vocabCountsMap[row[CoursesTable.deckId]] ?: 0L).toInt(),
                studying_user_count = getStudyingUserCount(courseId),
                favorite_user_count = getFavoriteUserCount(courseId),
                created_at = row[CoursesTable.createdAt].toString(),
                completed = completed
            )
        }

        val finalResults = if (isRevelant && queryList != null) {
            val idOrder = queryList!!.map { it.id }.withIndex().associate { it.value to it.index }
            results.sortedBy { idOrder[it.id] }
        } else results

        val lastItem = finalResults.lastOrNull()
        val nextCursorRes = if (finalResults.size == limit && lastItem != null) lastItem.id else null

        return AllCoursePaginationResponse(finalResults, searchInfo, nextCursorRes, totalCount)
    }

    suspend fun getAllCourses(userId: Int, searchInfo: SearchInfo, nextCursor: Long?, isOwner: Boolean = false): AllCoursePaginationResponse = dbQuery {
        val baseQuery = CoursesTable.innerJoin(UsersTable).leftJoin(TopicsTable)
            .select { if (isOwner) CoursesTable.creatorId eq userId else CoursesTable.privacy eq PrivacyType.PUBLIC }
        executeCoursePagination(userId, searchInfo, nextCursor, baseQuery, isOwner = isOwner)
    }

    suspend fun getMyCourses(userId: Int, searchInfo: SearchInfo, nextCursor: Long?): AllCoursePaginationResponse =
        getAllCourses(userId, searchInfo, nextCursor, true)

    suspend fun getFavoriteCourses(userId: Int, searchInfo: SearchInfo, nextCursor: Long?): AllCoursePaginationResponse = dbQuery {
        val baseQuery = UserFavoriteCoursesTable
            .innerJoin(CoursesTable, { UserFavoriteCoursesTable.courseId }, { CoursesTable.id })
            .innerJoin(UsersTable, { CoursesTable.creatorId }, { UsersTable.id })
            .leftJoin(TopicsTable, { CoursesTable.topicId }, { TopicsTable.id })
            .select { (UserFavoriteCoursesTable.userId eq userId) and (CoursesTable.privacy eq PrivacyType.PUBLIC) }
        executeCoursePagination(userId, searchInfo, nextCursor, baseQuery, isFavoriteList = true)
    }

    suspend fun getLearningCourses(userId: Int, searchInfo: SearchInfo, nextCursor: Long?): AllCoursePaginationResponse = dbQuery {
        val baseQuery = CoursesTable
            .innerJoin(UsersTable, { CoursesTable.creatorId }, { UsersTable.id })
            .leftJoin(TopicsTable, { CoursesTable.topicId }, { TopicsTable.id })
            .select {
                (CoursesTable.privacy eq PrivacyType.PUBLIC) and exists(
                    SpeakingDaysTable
                        .innerJoin(SpeakingParagraphsTable)
                        .innerJoin(SpeakingParagraphResultsTable)
                        .slice(intLiteral(1))
                        .select { (SpeakingDaysTable.courseId eq  CoursesTable.id) and
                                (SpeakingParagraphResultsTable.userId eq userId)}
                )
            }
        executeCoursePagination(userId, searchInfo, nextCursor, baseQuery)
    }


    suspend fun getTopics(): List<TopicDto> = dbQuery {
        TopicsTable
            .selectAll()
            .map { row ->
                TopicDto(
                    id = row[TopicsTable.id].value,
                    name = row[TopicsTable.name] ?: "",
                    colorHex = row[TopicsTable.color] ?: "#000000"
                )
            }
    }


    suspend fun getCourseDetail(userId: Int, courseId: Long): CourseDetailDto? = dbQuery {

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

        val list_speaking_day = speakingDayRepository.getSpeakingDays(userId, courseId, null)

        val list_topic = TopicsTable
            .selectAll()
            .map {row ->
                TopicDto(
                    id = row[TopicsTable.id].value,
                    name = row[TopicsTable.name] ?: "",
                    colorHex = row[TopicsTable.color] ?: "#000000"
                )
            }

        // Trả về DTO cuối cùng
        CourseDetailDto(
            id = courseIdEntity.value,
            thumbnail_url = row[CoursesTable.thumbnailUrl],
            creator = CreatorDto(
                id = row[UsersTable.id].value,
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
            deckId = row[CoursesTable.deckId]?.value ?: null,
            list_speaking_day = list_speaking_day,
            list_topic = list_topic
        )
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

    suspend fun createCourse(userId: Int, course: CreateCourseRequest): Long = dbQuery {
        CoursesTable.insertAndGetId {
            it[topicId] = course.topicId
            it[title] = course.title
            it[description] = course.description
            it[privacy] = api.models.enum.PrivacyType.valueOf(course.privacy)
            it[thumbnailUrl] = course.thumbnailUrl
            it[creatorId] = userId
        }.value
    }
    suspend fun editCourse(courseId: Long, userId: Int, course: EditCourseRequest): Boolean = dbQuery {
        val existingCourse = CoursesTable
            .select { CoursesTable.id eq courseId }
            .singleOrNull()
            ?: throw IllegalArgumentException(("Không tìm thấy khóa học này trong hệ thống"))

        val creatorIdInDb = existingCourse[CoursesTable.creatorId].value
        if (creatorIdInDb != userId) {
            throw IllegalArgumentException(("Bạn không có quyền chỉnh sửa khóa học của người khác"))
        }

        val oldThumbnailUrl = existingCourse[CoursesTable.thumbnailUrl]

        val updatedRows = CoursesTable.update({ CoursesTable.id eq courseId }) {
            it[topicId] = course.topicId
            it[title] = course.title
            it[description] = course.description
            it[privacy] = api.models.enum.PrivacyType.valueOf(course.privacy.uppercase().trim())
            it[thumbnailUrl] = course.thumbnailUrl
            it[updatedAt] = java.time.LocalDateTime.now()
        }

        if (updatedRows > 0) {
            if (!oldThumbnailUrl.isNullOrEmpty())
                CloudinaryService.deleteImage(oldThumbnailUrl)
            true
        } else {
            false
        }
    }
    suspend fun deleteCourse(courseId: Long, userId: Int): Boolean = dbQuery {

        val existingCourse = CoursesTable
            .select { CoursesTable.id eq courseId }
            .singleOrNull()
            ?: throw IllegalArgumentException(("Không tìm thấy khóa học này trong hệ thống"))

        val thumbnailUrl = existingCourse[CoursesTable.thumbnailUrl]

        val creatorIdInDb = existingCourse[CoursesTable.creatorId].value
        if (creatorIdInDb != userId) {
            throw IllegalArgumentException(("Bạn không có quyền xóa khóa học của người khác"))
        }
        val deletedRows = CoursesTable.deleteWhere { CoursesTable.id eq courseId }

        if (deletedRows > 0) {
            if (!thumbnailUrl.isNullOrEmpty())
                CloudinaryService.deleteImage(thumbnailUrl)
            true
        } else {
            false
        }
    }


    suspend fun addFavoriteCourse(userId: Int, courseId: Long): Boolean = dbQuery {

        val isAlreadyFavorited = UserFavoriteCoursesTable
            .select {
                (UserFavoriteCoursesTable.userId eq userId) and
                        (UserFavoriteCoursesTable.courseId eq courseId)
            }
            .singleOrNull() != null

        if (!isAlreadyFavorited) {
            UserFavoriteCoursesTable.insert {
                it[this.userId] = userId
                it[this.courseId] = courseId
            }
        }

        true
    }

    suspend fun removeFavoriteCourse(userId: Int, courseId: Long): Boolean = dbQuery {

        val deletedRows = UserFavoriteCoursesTable.deleteWhere {
            (UserFavoriteCoursesTable.userId eq userId) and
                    (UserFavoriteCoursesTable.courseId eq courseId)
        }

        true
    }
    suspend fun getLearnersInCourse(courseId: Long): List<Int> = dbQuery {
        (SpeakingParagraphResultsTable
                innerJoin SpeakingParagraphsTable
                innerJoin SpeakingDaysTable)
            .slice(SpeakingParagraphResultsTable.userId)
            .select { SpeakingDaysTable.courseId eq courseId }
            .withDistinct()
            .map { row ->
                row[SpeakingParagraphResultsTable.userId].value
            }
    }
    suspend fun getLearnersInCourseBySpeakingId(speakingId: Long): List<Int> = dbQuery {
        SpeakingParagraphResultsTable
            .innerJoin(SpeakingParagraphsTable)
            .slice(SpeakingParagraphResultsTable.userId)
            .select { SpeakingParagraphsTable.speakingDayId eq speakingId }
            .map { row ->
                row[SpeakingParagraphResultsTable.userId].value
            }
            .distinct()
    }
}
