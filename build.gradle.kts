plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.lexa"
version = "0.0.1"
val ktor_version = "2.3.x"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    // 1. Database Driver (Cầu nối để Kotlin nói chuyện được với PostgreSQL)
    implementation("org.postgresql:postgresql:42.7.2")

    // 2. Connection Pool (HikariCP - Quản lý hàng chờ kết nối siêu mượt)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // 3. ORM Exposed (Để viết câu lệnh SQL bằng Kotlin)
    val exposedVersion = "0.45.0"
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    // Thêm plugin ghi log của Ktor
    implementation("io.ktor:ktor-server-call-logging-jvm")

    // Đảm bảo bạn có thư viện Logback để in ra console
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.ktor:ktor-server-status-pages-jvm")

    implementation("org.mindrot:jbcrypt:0.4")

    // OAuth
    implementation("com.google.api-client:google-api-client:2.2.0")

    // Mail Service
    implementation("com.sun.mail:jakarta.mail:2.0.1")

    // Engine CIO cho Client
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    // Plugin xử lý JSON
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    // Thư viện Serialization cơ bản
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.x")

    // Đảm bảo bạn cũng có core client nếu chưa có
    implementation("io.ktor:ktor-client-core:${ktor_version}")
}
