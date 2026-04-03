package api.plugins

import api.repository.AuthRepository
import api.repository.CoursesRepository
import api.repository.DeckRepository
import api.repository.FlashcardRepository
import api.repository.ParagraphRepository
import api.repository.SpeakingDayRepository
import api.repository.ProfileRepository
import api.routes.authRoutes
import api.routes.deckRoutes
import api.routes.flashcardRoutes
import api.routes.paragraphRoute
import api.routes.speakingDayRoutes
import api.routes.profileRoutes
import api.services.AuthService
import api.services.DeckService
import api.services.FlashcardService
import api.services.ParagraphService
import api.services.SpeakingDayService
import api.services.ProfileService
import api.routes.courseRoutes
import api.services.CoursesService
import io.ktor.server.application.*
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import java.io.File

fun Application.configureRouting() {
    routing {
        courseRoutes(service = CoursesService(CoursesRepository()))
        authRoutes(authService = AuthService(AuthRepository()))
        deckRoutes(service = DeckService(DeckRepository()))
        flashcardRoutes(service = FlashcardService(FlashcardRepository()))
        speakingDayRoutes(service = SpeakingDayService(SpeakingDayRepository()))
        profileRoutes(service = ProfileService(ProfileRepository()))
        paragraphRoute(service = ParagraphService(ParagraphRepository()))
        staticFiles("/uploads", File("uploads")) {
            enableAutoHeadResponse()
        }
    }

//    routing {
//        aRoutes(aService = AService(ARespository()))
//    }
}
