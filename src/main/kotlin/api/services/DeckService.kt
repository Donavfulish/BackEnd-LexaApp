package api.services

import api.models.dto.CreateDeckRequest
import api.models.dto.DeckDto
import api.models.dto.DeckResult
import api.models.dto.UpdateDeckRequest
import api.repository.DeckRepository

class DeckService(
    private val deckRepository: DeckRepository
) {
    suspend fun getAllDecks(): List<DeckDto>{
        return deckRepository.getAllDecks(null);
    }

    suspend fun getMyDecks(userId: Int): List<DeckDto>{
        return deckRepository.getAllDecks(userId);
    }

    suspend fun getDeckResult(userId: Int, deckId: Long): Result<DeckResult?> {
        return try{
            val result = deckRepository.getDeckResult(userId, deckId)
            Result.success(result)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun addDeck(request: CreateDeckRequest) : Result<Long>{
        if (request.title.isBlank()) {
            return Result.failure(Exception("Tên bộ từ vựng không được để trống"))
        }

        return try {
            val id = deckRepository.createDeck(request);
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDeck(request: UpdateDeckRequest) : Result<Boolean>{
        if (request.title.isBlank()) {
            return Result.failure(Exception("Tên bộ từ vựng không được để trống"))
        }
        return try {
            val id = deckRepository.updateDeck(request);
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDeck(deckId: Long) : Result<Boolean>{
        return try {
            val id = deckRepository.deleteDeck(deckId);
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}