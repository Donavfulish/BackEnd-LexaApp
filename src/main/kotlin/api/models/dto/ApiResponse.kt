package api.models.dto

import kotlinx.serialization.Serializable

// ==========================================
// RESPONSE THÀNH CÔNG
// ==========================================
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

// ==========================================
// RESPONSE LỖI
// ==========================================
@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val message: String,
    val data: String? = null
)

// ==========================================
// HELPER FUNCTIONS
// ==========================================

fun <T> successResponse(
    data: T,
    message: String = "Thành công"
): ApiResponse<T> {
    return ApiResponse(true, message, data)
}

fun successResponse(
    message: String = "Thành công"
): ApiResponse<Unit> {
    return ApiResponse(true, message, null)
}

fun errorResponse(message: String): ApiResponse<Unit> {
    return ApiResponse(false, message, null)
}