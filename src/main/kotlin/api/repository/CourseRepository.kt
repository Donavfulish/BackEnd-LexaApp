package com.lexa.api.repository

import com.lexa.api.config.DatabaseFactory.dbQuery
import com.lexa.api.models.dto.CourseDto
import com.lexa.api.models.tables.CoursesTable
import org.jetbrains.exposed.sql.selectAll

class CourseRepository {
    suspend fun getAllCourses(): List<CourseDto> {
        return dbQuery {
            CoursesTable.selectAll().map { row ->
                CourseDto(
                    id = row[CoursesTable.id],
                    title = row[CoursesTable.title],
                    description = row[CoursesTable.description],
                    topic = row[CoursesTable.topic]
                )
            }
        }
    }
}