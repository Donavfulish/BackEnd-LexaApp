package api.services

import api.models.dto.GetProfileResponse
import api.models.dto.UpdateProfileRequest
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
}