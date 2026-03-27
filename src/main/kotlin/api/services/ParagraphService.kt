package api.services

import api.models.dto.CreateParagraphRequest
import api.models.dto.ParagraphResponseDto
import api.models.dto.ParagraphResultResponseDto
import api.models.dto.UpdateParagraphRequest
import api.models.dto.UpdateParagraphResultRequest
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

    suspend fun updateParagraphInfo(
        paragraphId: Long,
        request: UpdateParagraphRequest,
        userRole: String
    ): Result<ParagraphResponseDto> {
        if (userRole != "teacher") {
            return Result.failure(Exception("FORBIDDEN_ROLE"))
        }

        return try {
            val isUpdated = paragraphRepository.updateParagraphInfo(paragraphId, request)
            if (isUpdated) {
                // Lấy lại data mới nhất từ DB để trả về cho Client
                val updatedData = paragraphRepository.getParagraphById(paragraphId)
                    ?: return Result.failure(Exception("Không tìm thấy dữ liệu sau khi cập nhật"))
                Result.success(updatedData)
            } else {
                Result.failure(Exception("Không tìm thấy paragraph với ID này để cập nhật"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun deleteParagraph(
        paragraphId: Long,
        userRole: String
    ): Result<Unit> {
        if (userRole != "teacher") {
            return Result.failure(Exception("FORBIDDEN_ROLE"))
        }

        return try {
            val isDeleted = paragraphRepository.deleteParagraph(paragraphId)
            if (isDeleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Không tìm thấy paragraph với ID này để xóa"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateParagraphResult(
        userId: Int,
        request: UpdateParagraphResultRequest
    ): Result<ParagraphResultResponseDto> {

        if (request.paragraphId <= 0) {
            return Result.failure(Exception("ID đoạn văn không hợp lệ"))
        }

        return try {
            val result = paragraphRepository.upsertParagraphResult(userId, request)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}