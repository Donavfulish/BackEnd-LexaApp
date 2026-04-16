package api.services

import api.repository.ProfileRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MulticastMessage

class NotificationService(private val profileRepository: ProfileRepository) {


    suspend fun sendReminderNotification(userId: Int, title: String, body: String): Boolean {
        return try {

            val fcmToken = profileRepository.getFcmToken(userId)

            if (fcmToken.isNullOrEmpty()) {
                println("User $userId không có FCM Token")
                return false
            }

            val message = Message.builder()
                .putData("title", title)
                .putData("body", body)
                .putData("type", "REMINDER")
                .setToken(fcmToken)
                .build()


            val response = FirebaseMessaging.getInstance().send(message)
            println("Gửi thành công, Message ID: $response")
            true

        } catch (e: Exception) {
            println("Lỗi khi gửi push: ${e.message}")
            false
        }
    }

    suspend fun sendMulticastNotification(userIds: List<Int>, title: String, body: String) {
        if (userIds.isEmpty()) return

        val tokens = profileRepository.getFcmTokensByUserIds(userIds)
        if (tokens.isEmpty()) return

        val tokenChunks = tokens.chunked(500)

        for (chunk in tokenChunks) {
            val message = MulticastMessage.builder()
                .putData("title", title)
                .putData("body", body)
                .addAllTokens(chunk)
                .build()

            try {
                val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
                println("Đã gửi thành công ${response.successCount} tin nhắn, thất bại ${response.failureCount}.")
            } catch (e: Exception) {
                println("Lỗi khi gửi Multicast: ${e.message}")
            }
        }
    }

}