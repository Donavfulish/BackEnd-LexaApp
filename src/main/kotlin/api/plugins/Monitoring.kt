package com.lexa.api.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        // Đặt mức độ log là INFO để theo dõi các luồng bình thường
        level = Level.INFO

        // Bạn có thể tùy chỉnh định dạng dòng log in ra cho dễ đọc
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val path = call.request.uri
            "[$httpMethod] $path -> Về đích với Status: $status"
        }
    }
}