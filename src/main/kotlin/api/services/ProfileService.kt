package api.services

import api.models.dto.ChangePasswordRequest
import api.models.dto.GetAchievementResponse
import api.models.dto.GetProfileResponse
import api.models.dto.UpdateFcmTokenRequest
import api.models.dto.UpdateProfileRequest
import api.models.dto.UserInfo
import api.repository.ProfileRepository


class ProfileService(
    private val profileRepository: ProfileRepository
) {
    suspend fun getProfile(userId: Int): GetProfileResponse{
        return profileRepository.getProfile(userId);
    }

    suspend fun updateProfile(userId: Int, data: UpdateProfileRequest): Boolean {
        return profileRepository.updateProfile(userId, data)
    }

    suspend fun updateFcmToken(userId: Int, request: UpdateFcmTokenRequest): Boolean {
        return profileRepository.updateFcmToken(userId, request)
    }

    suspend fun uploadAvatar(userId: Int, imageUrl: String?): Boolean {
        return profileRepository.updateAvatar(userId, imageUrl)
    }
    suspend fun getAchievement(userId: Int): GetAchievementResponse {
        return profileRepository.getAchievements(userId)
    }
}