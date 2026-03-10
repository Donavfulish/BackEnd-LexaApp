package com.lexa.api.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import io.github.cdimascio.dotenv.dotenv

object DatabaseFactory {
    private val env = dotenv()

    fun init() {

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"

            jdbcUrl = env["DB_URL"]
            username = env["DB_USER"]
            password = env["DB_PASSWORD"]

            maximumPoolSize = env["DB_MAX_POOL"]?.toInt() ?: 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // fix connection drop
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000

            validate()
        }
        Database.connect(HikariDataSource(config))
    }

    // Hàm tiện ích để bọc các câu lệnh SQL chạy bất đồng bộ (chạy trong coroutine thay vì block main thread của ktor)
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction { block() }
}