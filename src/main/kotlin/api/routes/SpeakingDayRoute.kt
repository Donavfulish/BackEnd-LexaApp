package api.routes


import api.config.getUserId
import api.models.dto.ApiResponse
import api.models.dto.CreateCourseRequest
import api.models.dto.CreateSpeakingDayRequest
import api.models.dto.EditCourseRequest
import api.models.dto.EditSpeakingDayRequest
import api.models.dto.errorResponse
import api.models.dto.successResponse
import api.services.SpeakingDayService
import com.lexa.api.services.CoursesService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.speakingDayRoutes(speakingDayService: SpeakingDayService) {

    route("/api/speaking-day/{speakingDayId}") {

        get {
            val speakingDayId: Long = call.parameters["speakingDayId"]!!.toLong()
           val speakingDay = speakingDayService.getParagraphSpeakingDay(speakingDayId);
            call.respond(
                HttpStatusCode.OK,
                successResponse(speakingDay, "Lấy danh sách paragraph cua speaking day thành công")
            )
        }


    }
    route("/api/users/me/speaking-day") {

        authenticate("auth-jwt") {
            post {
                val userId = call.getUserId() ?: return@post call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val speakingDay = call.receive<CreateSpeakingDayRequest>()

                speakingDayService.addSpeakingDay(userId,  speakingDay).fold(
                    onSuccess = { newId ->
                        call.respond(
                            HttpStatusCode.Created,
                            successResponse(newId, "Tạo bài học thành công")
                        )
                    },
                    onFailure = { e ->
                        call.respond(
                            HttpStatusCode.BadRequest,
                            errorResponse(e.message ?: "Đã có lỗi xảy ra khi tạo bài học")
                        )
                    }
                )
            }

            patch ("/{speakingDayId}"){
                val userId = call.getUserId() ?: return@patch call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val speakingDayId = call.parameters["speakingDayId"]?.toLongOrNull() ?: return@patch call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID bài học không hợp lệ")
                )

                val speakingDay = call.receive<EditSpeakingDayRequest>()

                speakingDayService.editSpeakingDay(userId, speakingDayId, speakingDay).fold(
                    onSuccess = {
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(null, "Cập nhật bài học thành công")
                        )
                    },
                    onFailure = { e ->
                        call.respond(
                            HttpStatusCode.Forbidden, // Dùng 403 Forbidden cho lỗi sai quyền sở hữu
                            errorResponse(e.message ?: "Lỗi cập nhật bài học")
                        )
                    }
                )
            }

            delete("/{speakingDayId}") {
                val userId = call.getUserId() ?: return@delete call.respond(
                    HttpStatusCode.Unauthorized, errorResponse("Không thể xác thực người dùng. Vui lòng đăng nhập lại.")
                )

                val speakingDayId = call.parameters["speakingDayId"]?.toLongOrNull() ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, errorResponse("ID bài học không hợp lệ")
                )

                speakingDayService.deleteSpeakingDay(userId, speakingDayId).fold(
                    onSuccess = {
                        call.respond(
                            HttpStatusCode.OK,
                            successResponse(null, "Xóa bài học thành công")
                        )
                    },
                    onFailure = { e ->
                        call.respond(
                            HttpStatusCode.Forbidden,
                            errorResponse(e.message ?: "Lỗi xóa bài học")
                        )
                    }
                )
            }
        }

    }
}
