package api
import api.plugins.configureExceptionHandling
import api.plugins.configureRouting
import com.lexa.api.plugins.configureSecurity
import com.lexa.api.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import com.lexa.api.config.DatabaseFactory
import com.lexa.api.plugins.applicationHttpClient
import com.lexa.api.plugins.configureMonitoring
import io.github.cdimascio.dotenv.dotenv


// Mở cmd chạy lệnh dưới sau khi chạy Frontend + Backend
// adb reverse tcp:8081 tcp:8081

fun main(args: Array<String>) {
    try {
        val process = Runtime.getRuntime().exec(arrayOf("adb", "reverse", "tcp:8081", "tcp:8081"))
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            println("--- ADB Reverse: Success (Port 8081) ---")
        }
    } catch (e: Exception) {
        println("--- ADB Reverse: Skipped (Không tìm thấy thiết bị hoặc ADB chưa cài đặt) ---")
    }

    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init()
    configureMonitoring()
    configureSerialization()
    configureSecurity(httpClient = applicationHttpClient)
    configureRouting()
    configureExceptionHandling()
}

