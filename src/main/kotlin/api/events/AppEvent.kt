package api.events

sealed class AppEvent {
    // Sự kiện liên quan đến khóa học
    data class CourseUpdated(val userIds: List<Int>,  val title: String, val body: String) : AppEvent()

    // Sự kiện liên quan đến ngày học
    data class SpeakingDayChanged(val userIds: List<Int>,   val title: String, val body: String) : AppEvent()


}