package api.utils

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object FileUtil {
    // 1. Hàm tạo tên file Unique sử dụng UUID
    fun generateUniqueFileName(prefix: String, originalFileName: String?): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss"))
        val uuid = UUID.randomUUID().toString().take(8)
        val extension = originalFileName?.substringAfterLast(".", "jpg") ?: "jpg"

        return "${prefix}_${timestamp}_${uuid}.${extension}"
    }

    // 2. Hàm lưu mảng Byte vào thư mục
    fun saveFileToDisk(bytes: ByteArray, fileName: String): String {
        val uploadDir = File("uploads/certificates")
        if (!uploadDir.exists()) uploadDir.mkdirs()

        val file = File(uploadDir, fileName)
        file.writeBytes(bytes)

        return "uploads/certificates/$fileName"
    }
}