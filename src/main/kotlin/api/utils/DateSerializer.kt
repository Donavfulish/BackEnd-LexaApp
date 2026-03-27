package api.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// Tạo một Object để cấu hình cách format Date
object DateSerializer : KSerializer<Date> {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd") // Format bạn muốn trả về

    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        // Biến Date thành String khi trả JSON về Client
        encoder.encodeString(dateFormat.format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        // Biến String từ Client gửi lên thành Date
        return dateFormat.parse(decoder.decodeString())
    }
}

fun Date.toLocalDate(): LocalDate {
    return this.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}