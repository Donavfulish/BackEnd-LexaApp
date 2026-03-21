package com.lexa.api.plugins

import api.repository.AuthRepository
import api.repository.CoursesRepository
import api.repository.DeckRepository
import api.repository.FlashcardRepository
import api.repository.ProfileRepository
import api.routes.authRoutes
import api.routes.deckRoutes
import api.routes.flashcardRoutes
import api.routes.profileRoutes
import api.services.AuthService
import api.services.DeckService
import api.services.FlashcardService
import api.services.ProfileService
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
        profileRoutes(profileService = ProfileService(ProfileRepository()))

    }

//    routing {
//        aRoutes(aService = AService(ARespository()))
//    }
}
