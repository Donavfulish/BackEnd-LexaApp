package com.lexa.api.models.tables

import org.jetbrains.exposed.sql.Table

object CoursesTable : Table("courses") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val description = text("description")
    val topic = varchar("topic", 100)

    override val primaryKey = PrimaryKey(id)
}