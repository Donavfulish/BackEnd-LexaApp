package com.lexa.api.plugins

import api.repository.AuthRepository
import api.repository.CoursesRepository
import api.repository.DeckRepository
import api.repository.FlashcardRepository
import api.routes.authRoutes
import api.routes.deckRoutes
import api.routes.flashcardRoutes
import api.services.AuthService
import api.services.DeckService
import api.services.FlashcardService
import com.lexa.api.routes.courseRoutes
import com.lexa.api.services.CoursesService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        courseRoutes(coursesService = CoursesService(CoursesRepository()))
        authRoutes(authService = AuthService(AuthRepository()))
        deckRoutes(deckService = DeckService(DeckRepository()))
        flashcardRoutes(flashcardService = FlashcardService(FlashcardRepository()))
    }

//    routing {
//        aRoutes(aService = AService(ARespository()))
//    }
}
