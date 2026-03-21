package api.services

import api.models.dto.GetProfileResponse
import api.repository.ProfileRepository


class ProfileService(
    private val profileRepository: ProfileRepository
) {
    suspend fun getProfile(userId: Int): GetProfileResponse{
        return profileRepository.getProfile(userId);
    }


}