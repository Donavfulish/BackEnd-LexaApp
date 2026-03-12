package api.repository

import api.models.dto.CourseDto
import api.models.dto.CreateCourseRequest
import api.models.tables.CoursesTable
import com.lexa.api.config.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll

// Nơi giao tiếp trực tiếp với database (nơi này sẽ được sử dụng models và dto)
class CoursesRepository {
    suspend fun getAllCourses(): List<CourseDto> = dbQuery {
        CoursesTable
            .selectAll()
            .map { row ->
                CourseDto(
                    id = row[CoursesTable.id].value,
                    topicId = row[CoursesTable.topicId]?.value,
                    title = row[CoursesTable.title],
                    description = row[CoursesTable.description],
                    creatorId = row[CoursesTable.creatorId].value,
                    privacy = row[CoursesTable.privacy]?.name
                )
            }
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
}