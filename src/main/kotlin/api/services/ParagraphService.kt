package api.services

import api.models.dto.CreateParagraphRequest
import api.models.dto.ParagraphResponseDto
import api.repository.ParagraphRepository

class ParagraphService(
    private val paragraphRepository: ParagraphRepository
) {

    suspend fun createParagraph(request: CreateParagraphRequest, userRole: String): Result<ParagraphResponseDto> {
        // 1. Validate Role
        if (userRole != "teacher") {
            return Result.failure(Exception("FORBIDDEN_ROLE"))
        }

        // 2. Validate dữ liệu đầu vào cơ bản
        if (request.paragraph.isBlank()) {
            return Result.failure(Exception("Nội dung đoạn văn không được để trống"))
        }
        request.paragraphOrder?.let {
            if (it <= 0) {
                return Result.failure(Exception("Thứ tự đoạn văn không hợp lệ"))
            }
        }

        // 3. Gọi DB
        return try {
            val result = paragraphRepository.createParagraph(request)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}