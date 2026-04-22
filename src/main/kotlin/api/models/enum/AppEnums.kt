package api.models.enum

import api.models.enum.SortBy.CREATED

enum class LanguageType { ENGLISH, VIETNAMESE }
enum class OtpPurpose { VERIFY_EMAIL, RESET_PASSWORD }
enum class PrivacyType { PUBLIC, PRIVATE }
enum class ProgressStatus { REMEMBER, FORGOTTEN }
enum class ProviderType { GOOGLE, FACEBOOK, GITHUB }
enum class UserRole { TEACHER, STUDENT }
enum class VocabType { NONE, A1, A2, B1, B2, C1, C2 }

enum class SortBy(val str: String) {
    TITLE("title"),
    CREATED("created");
    companion object {
        fun fromString(value: String?): SortBy {
            return entries.find { it.str.equals(value, ignoreCase = true) } ?: CREATED
        }
    }
}

enum class OrderBy(val str: String) {
    ASC("asc"),
    DESC("desc");
    companion object{
        fun fromString(value: String?): OrderBy {
            return entries.find { it.str.equals(value, ignoreCase = true) } ?: DESC
        }
    }
}