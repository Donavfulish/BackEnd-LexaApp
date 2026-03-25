package com.lexa

import api.plugins.configureExceptionHandling
import com.lexa.api.plugins.configureRouting
import com.lexa.api.plugins.configureSecurity
import com.lexa.api.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import com.lexa.api.config.DatabaseFactory
import com.lexa.api.plugins.applicationHttpClient
import com.lexa.api.plugins.configureMonitoring
import io.ktor.client.HttpClient

fun main(args: Array<String>) {
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

