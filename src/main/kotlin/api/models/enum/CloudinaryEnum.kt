package api.models.enum

val LEXA_FOLDER_STRING = "lexa"

enum class CloudinaryFolder(val path: String) {
    PROFILE("${LEXA_FOLDER_STRING}/profile"),
    CERTIFICATES("${LEXA_FOLDER_STRING}/certificates"),
    FLASHCARD("${LEXA_FOLDER_STRING}/flashcard"),
    COURSE("${LEXA_FOLDER_STRING}/course")
}