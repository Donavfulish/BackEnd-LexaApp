package api.services

import api.models.dto.AllCoursePaginationResponse
import api.models.dto.AllDeckPaginationResponse
import api.models.dto.CreateDeckRequest
import api.models.dto.CreateDeckResultRequest
import api.models.dto.DeckDto
import api.models.dto.DeckResult
import api.models.dto.SearchInfo
import api.models.dto.ShortCourseDto
import api.models.dto.UpdateDeckRequest
import api.models.dto.UpdateDeckResultRequest
import api.repository.DeckRepository

class DeckService(
    private val deckRepository: DeckRepository
) {

    suspend fun getMyDecks(userId: Int, searchInfo: SearchInfo, nextCursor: Long?): AllDeckPaginationResponse{
        return deckRepository.getAllDecks(userId, searchInfo, nextCursor);
    }

    suspend fun getDeckResult(userId: Int, deckId: Long): Result<DeckResult?> {
        return try{
            val result = deckRepository.getDeckResult(userId, deckId)
            Result.success(result)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun addDeckResult(userId: Int, request: CreateDeckResultRequest): Result<Boolean>{
        return try {
            if(userId != request.userId){
                return Result.failure(Exception("Không phải chủ bộ từ vựng"))            }
            val bool = deckRepository.createDeckResult(request)
            Result.success(bool)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun updateDeckResult(userId: Int, request: UpdateDeckResultRequest): Result<Boolean>{
        return try {
            if(userId == null){
                return Result.failure(Exception("Không hợp lệ"))            }
            val bool = deckRepository.updateDeckResult(request, userId)
            Result.success(bool)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    suspend fun getFavoriteDecks(userId: Int): List<ShortCourseDto> {
        return deckRepository.getFavoriteDecks(userId)
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

    suspend fun updateDeck(userId: Int, request: UpdateDeckRequest) : Result<Boolean>{
//        if (request.title.isNullOrEmpty()) {
//            return Result.failure(Exception("Tên bộ từ vựng không được để trống"))
//        }
        return try {
            val id = deckRepository.updateDeck(userId, request);
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDeck(userId: Int, deckId: Long) : Result<Boolean>{
        return try {
            val id = deckRepository.deleteDeck(userId, deckId);
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun favoriteDeck(userId: Int, deckId: Long): Result<Boolean> {
        return try {
            val result = deckRepository.addFavoriteDeck(userId, deckId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disFavoriteDeck(userId: Int, deckId: Long): Result<Boolean> {
        return try {
            val result = deckRepository.removeFavoriteDeck(userId, deckId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}