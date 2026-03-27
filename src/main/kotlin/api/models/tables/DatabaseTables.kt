package api.models.tables

import api.models.enum.* // Lưu ý: Nếu báo đỏ, bạn check lại xem tên thư mục là 'enum' hay 'enums' nhé
import api.utils.jsonb
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.postgresql.util.PGobject

// Helper function để ép kiểu Enum của Kotlin sang đúng Custom Enum của PostgreSQL
inline fun <reified T : Enum<T>> org.jetbrains.exposed.sql.Table.pgEnum(
    columnName: String,
    postgresEnumName: String
): org.jetbrains.exposed.sql.Column<T> = customEnumeration(
    name = columnName,
    sql = postgresEnumName,
    fromDb = { value -> enumValueOf<T>(value.toString()) }, // Đọc từ DB lên
    toDb = { enumValue ->
        // Gói String thành PGobject trước khi gửi xuống DB
        PGobject().apply {
            type = postgresEnumName
            value = enumValue.name
        }
    }
)

// ==========================================
// BẢNG ĐỘC LẬP & LƯU TRỮ CHÍNH
// ==========================================

object UsersTable : IntIdTable("users") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable().uniqueIndex()
    val passwordHash = text("password_hash").nullable()
    val emailVerified = bool("email_verified").default(false)
    val address = text("address").nullable()
    val dateOfBirth = date("date_of_birth").nullable()
    val avatarUrl = text("avatar_url").nullable()
    val languageCertificate = text("language_certificate").nullable()
    val teachingDegree = text("teaching_degree").nullable()

    // Đã sửa thành pgEnum
    val role = pgEnum<UserRole>("role", "user_role").default(UserRole.STUDENT)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object TopicsTable : IntIdTable("topics") {
    val name = varchar("name", 255).nullable()
    val color = varchar("color", 255).nullable()
}

object PartOfSpeechesTable : IntIdTable("part_of_speeches") {
    val name = varchar("name", 255).nullable()
}

object SystemSettingsTable : IntIdTable("system_settings") {
    // Đã sửa thành pgEnum
    val language = pgEnum<LanguageType>("language", "language_type").default(LanguageType.VIETNAMESE)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

// ==========================================
// BẢNG LIÊN QUAN ĐẾN AUTHENTICATION
// ==========================================

object AuthProviderTable : LongIdTable("auth_provider") {
    val userId = reference("user_id", UsersTable)

    // Đã sửa thành pgEnum
    val provider = pgEnum<ProviderType>("provider", "provider_type").nullable()

    val providerUserId = varchar("provider_user_id", 255)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init { uniqueIndex("unique_provider_user", provider, providerUserId) }
}

object RefreshTokensTable : LongIdTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = text("token_hash")
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object UserOtpsTable : LongIdTable("user_otps") {
    val email = text("email")
    val otpCode = text("otp_code")

    // Đã sửa thành pgEnum
    val purpose = pgEnum<OtpPurpose>("purpose", "otp_purpose").default(OtpPurpose.VERIFY_EMAIL)

    val isUsed = bool("is_used").default(false)
    val expiresAt = datetime("expires_at")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

// ==========================================
// BẢNG LIÊN QUAN ĐẾN HỌC TẬP (COURSES, DECKS, FLASHCARDS)
// ==========================================

object FlashcardDecksTable : LongIdTable("flashcard_decks") {
    val creatorId = reference("creator_id", UsersTable)
    val title = varchar("title", 100)
    val description = text("description").nullable()
    val topicId = reference("topic_id", TopicsTable)

    // Đã sửa thành pgEnum
    val privacy = pgEnum<PrivacyType>("privacy", "privacy_type").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object CoursesTable : LongIdTable("courses") {
    val topicId = reference("topic_id", TopicsTable).nullable()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val creatorId = reference("creator_id", UsersTable)
    val deckId = reference("deck_id", FlashcardDecksTable).nullable()
    val thumbnailUrl = text("thumbnail_url").nullable()

    // Đã sửa thành pgEnum
    val privacy = pgEnum<PrivacyType>("privacy", "privacy_type").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object FlashcardsTable : LongIdTable("flashcards") {
    val deckId = reference("deck_id", FlashcardDecksTable)
    val imageUrl = text("image_url").nullable()
    val audioUrl = text("audio_url").nullable()
    val transcription = varchar("transcription", 255).nullable()
    val word = text("word")
    val meaningVi = text("meaning_vi").nullable()

    // Đã sửa thành pgEnum
    val type = pgEnum<VocabType>("type", "vocab_type").nullable()

    val partOfSpeechId = reference("part_of_speech_id", PartOfSpeechesTable).nullable()
    val example = text("example").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

object SpeakingDaysTable : LongIdTable("speaking_days") {
    val title = varchar("title", 255).nullable()
    val courseId = reference("course_id", CoursesTable).nullable()
    val dayOrder = long("day_order").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init { uniqueIndex("unique_course_day", courseId, dayOrder) }
}

object SpeakingParagraphsTable : LongIdTable("speaking_paragraphs") {
    val speakingDayId = reference("speaking_day_id", SpeakingDaysTable)
    val paragraph = text("paragraph").nullable()
    val audioUrl = text("audio_url").nullable()
    val paragraphOrder = long("paragraph_order").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init { uniqueIndex("unique_day_paragraph", speakingDayId, paragraphOrder) }
}

// ==========================================
// BẢNG RELATION & THỐNG KÊ (COMPOSITE PRIMARY KEYS)
// ==========================================

object UserFavoriteCoursesTable : Table("user_favorite_courses") {
    val userId = reference("user_id", UsersTable)
    val courseId = reference("course_id", CoursesTable)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(userId, courseId)
}

object UserFavoriteDecksTable : Table("user_favorite_decks") {
    val userId = reference("user_id", UsersTable)
    val deckId = reference("deck_id", FlashcardDecksTable)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(userId, deckId)
}

object DeckResultsTable : Table("deck_results") {
    val deckId = reference("deck_id", FlashcardDecksTable)
    val userId = reference("user_id", UsersTable)
    val rememberedCount = integer("remembered_count").nullable()
    val forgottenCount = integer("forgotten_count").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(deckId, userId)
}

object FlashcardResultsTable : Table("flashcard_results") {
    val userId = reference("user_id", UsersTable)
    val flashcardId = reference("flashcard_id", FlashcardsTable)

    // Đã sửa thành pgEnum
    val status = pgEnum<ProgressStatus>("status", "progress_status").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(userId, flashcardId)
}

object SpeakingParagraphResultsTable : Table("speaking_paragraph_results") {
    val userId = reference("user_id", UsersTable)
    val paragraphId = reference("paragraph_id", SpeakingParagraphsTable)
    val wordEvaluation = jsonb("word_evaluation").nullable()
    val goodCount = integer("good_count").nullable()
    val mediumCount = integer("medium_count").nullable()
    val badCount = integer("bad_count").nullable()
    val userAudioUrl = text("user_audio_url").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(userId, paragraphId)
}
