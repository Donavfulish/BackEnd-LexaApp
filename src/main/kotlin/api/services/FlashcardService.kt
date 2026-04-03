package api.services

import api.models.dto.CreateFlashcardRequest
import api.models.dto.DetailFlashcard
import api.models.dto.DetailFlashcardWithResult
import api.models.dto.UpdateFlashcardRequest
import api.models.dto.UpdateFlashcardResultRequest
import api.repository.FlashcardRepository

class FlashcardService(
    private val flashcardRepository: FlashcardRepository
) {
    suspend fun getAllFlashcard(deckId: Long): List<DetailFlashcard>{
        return flashcardRepository.getAllFlashcard(deckId);
    }

    suspend fun getAllFlashcardWithResult(deckId: Long, userId: Int): List<DetailFlashcardWithResult> {
        return flashcardRepository.getAllFlashcardWithResult(deckId, userId)
    }

    suspend fun addFlashcard(userId: Int, request: CreateFlashcardRequest): Result<Long> {
        if (request.word.isBlank()) {
            return Result.failure(Exception("Từ vựng không được để trống"))
        }
        return try {
            val id = flashcardRepository.createFlashcard(userId, request)
            if(id != -1.toLong()){
                Result.success(id)
            } else
            {
                Result.failure(Exception("Không phải chủ bộ từ vựng"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFlashcard(userId: Int, request: UpdateFlashcardRequest): Result<Boolean> {
        return try {
            val success = flashcardRepository.updateFlashcard(userId, request)
            if(success){
                Result.success(success)
            } else {
                Result.failure(Exception("Flashcard không tồn tại hoặc bạn không có quyền chỉnh sửa"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFlashcard(userId: Int, flashcardId: Long): Result<Boolean> {
        return try {
            val success = flashcardRepository.deleteFlashcard(userId, flashcardId)
            if(success){
                Result.success(success)
            } else {
                Result.failure(Exception("Flashcard không tồn tại hoặc bạn không có quyền xoá"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFlashcardResults(userId: Int, request: UpdateFlashcardResultRequest): Result<Boolean> {
        return try {
            val success = flashcardRepository.updateFlashcardResults(userId, request)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Lỗi khi cập nhật danh sách kết quả thẻ!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}