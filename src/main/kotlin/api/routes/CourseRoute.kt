package com.lexa.api.routes

import com.lexa.api.repository.CourseRepository
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseRoutes(repository: CourseRepository) {
    route("/api/courses") {
        get {
            val courses = repository.getAllCourses()
            call.respond(courses) // Ktor tự động parse List<CourseDto> thành chuỗi JSON
        }
    }
}