package api.services

import api.models.dto.CreateFlashcardRequest
import api.models.dto.DetailFlashcard
import api.models.dto.UpdateFlashcardRequest
import api.repository.FlashcardRepository

class FlashcardService(
    private val flashcardRepository: FlashcardRepository
) {
    suspend fun getAllFlashcard(deckId: Long): List<DetailFlashcard>{
        return flashcardRepository.getAllFlashcard(deckId);
    }

    suspend fun addFlashcard(request: CreateFlashcardRequest): Result<Long> {
        if (request.word.isBlank()) {
            return Result.failure(Exception("Từ vựng không được để trống"))
        }
        return try {
            val id = flashcardRepository.createFlashcard(request)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFlashcard(request: UpdateFlashcardRequest): Result<Boolean> {
        if (request.word.isBlank()) {
            return Result.failure(Exception("Từ vựng không được để trống"))
        }
        return try {
            val success = flashcardRepository.updateFlashcard(request)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFlashcard(flashcardId: Long): Result<Boolean> {
        return try {
            val success = flashcardRepository.deleteFlashcard(flashcardId)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}