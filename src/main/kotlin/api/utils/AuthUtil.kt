package api.utils

import api.models.dto.UserInfo
import api.models.dto.UserResponse
import org.mindrot.jbcrypt.BCrypt

object AuthUtil {
    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun verify(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }

    fun UserInfo.toResponse(): UserResponse {
        return UserResponse(
            id = id,
            email = email,
            name = name,
            role = role,
            isEmailVerified = isEmailVerified
        )
    }
}