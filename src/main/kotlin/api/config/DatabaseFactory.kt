package com.lexa.api.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = "jdbc:postgresql://localhost:5432/lexa_db" // Sửa lại thông tin DB của bạn
            username = "postgres"
            password = "password"
            maximumPoolSize = 10
        }
        Database.connect(HikariDataSource(config))
    }

    // Cấu hình HikariCP
    private fun hikari(): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"

            // Thay đổi user, password và tên database của team vào đây
            jdbcUrl = "jdbc:postgresql://localhost:5432/lexa_db"
            username = "postgres"
            password = "your_password"

            maximumPoolSize = 10 // Chỉ cho phép tối đa 10 kết nối cùng lúc để tránh sập DB
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // Validate connection (Đảm bảo kết nối không bị đứt ngầm)
            validate()
        }
        return HikariDataSource(config)
    }

    // Hàm tiện ích để bọc các câu lệnh SQL chạy bất đồng bộ
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction { block() }
}