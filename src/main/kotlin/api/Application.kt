package com.lexa

import api.plugins.configureExceptionHandling
import com.lexa.api.plugins.configureRouting
import com.lexa.api.plugins.configureSecurity
import com.lexa.api.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import com.lexa.api.config.DatabaseFactory
import com.lexa.api.plugins.configureMonitoring

fun main(args: Array<String>) {
    EngineMain.main(args)
}


fun Application.module() {
    DatabaseFactory.init()
    configureMonitoring()
    configureSerialization()
    configureSecurity()
    configureRouting()
    configureExceptionHandling()
}

