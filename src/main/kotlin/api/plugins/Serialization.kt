package com.lexa.api.plugins

import api.utils.DateSerializer
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.exposed.sql.javatime.Date
import java.time.LocalDate

fun Application.configureSerialization() {

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            }
        )
    }
}