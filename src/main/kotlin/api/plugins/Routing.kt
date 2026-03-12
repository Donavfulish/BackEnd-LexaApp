package com.lexa.api.plugins

import api.repository.CoursesRepository
import com.lexa.api.routes.courseRoutes
import com.lexa.api.services.CoursesService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        courseRoutes(coursesService = CoursesService(CoursesRepository()))
    }

//    routing {
//        aRoutes(aService = AService(ARespository()))
//    }
}
