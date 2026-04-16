package api.events

import api.services.NotificationService
import io.ktor.server.application.*
import kotlinx.coroutines.launch

fun Application.setupEventListeners(notificationService: NotificationService) {

    launch {
        EventBus.events.collect { event ->
            when (event) {
                is AppEvent.CourseUpdated -> {
                    notificationService.sendMulticastNotification(
                        userIds = event.userIds, // Truyền mảng vào
                        title = event.title,
                        body = event.body
                    )
                }
                is AppEvent.SpeakingDayChanged -> {
                    notificationService.sendMulticastNotification(
                        userIds = event.userIds,
                        title = event.title,
                        body = event.body
                    )
                }
            }
        }
    }
}