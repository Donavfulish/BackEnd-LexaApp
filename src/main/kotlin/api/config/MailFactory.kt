    package api.config

import io.github.cdimascio.dotenv.dotenv
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

data class MailConfig(
    val host: String,
    val port: Int,
    val email: String,
    val password: String
)

object MailFactory {
    private val env = dotenv()

    private val config = MailConfig(
        host = "smtp.gmail.com",
        port = 587,
        email = env["SMTP_USER"],
        password = env["SMTP_PASS"]
    )

    private val session: Session by lazy {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port)
            put("mail.smtp.port", config.port.toString())

            // Thêm dòng này để fix lỗi WRONG_VERSION_NUMBER
            put("mail.smtp.ssl.protocols", "TLSv1.2")
        }

        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(config.email, config.password)
            }
        })
    }

    suspend fun sendEmail(to: String, subject: String, body: String) = withContext(Dispatchers.IO) {
        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.email))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                this.subject = subject
                setText(body, "utf-8", "html") // Hỗ trợ gửi cả HTML nếu cần
            }
            Transport.send(message)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}