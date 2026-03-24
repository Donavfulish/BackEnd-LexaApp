package api.routes


import api.models.dto.ApiResponse
import api.models.dto.CreateCourseRequest
import api.models.dto.successResponse
import api.services.SpeakingDayService
import com.lexa.api.services.CoursesService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.speakingDayRoutes(speakingDaySerice: SpeakingDayService) {

    route("/api/speaking-day/{speakingDayId}") {

        get {
            val speakingDayId: Long = call.parameters["speakingDayId"]!!.toLong()
            val speakingDay = speakingDaySerice.getParagraphSpeakingDay(speakingDayId);
            call.respond(
                HttpStatusCode.OK,
                successResponse(speakingDay, "Lấy danh sách paragraph cua speaking day thành công")
            )
        }


    }

}
