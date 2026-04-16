package api.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    // extraBufferCapacity giúp lưu tạm sự kiện nếu người nghe xử lý chưa kịp
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    // Hàm dùng để đẩy sự kiện vào Bus (Không cần suspend để gọi cho dễ)
    fun publish(event: AppEvent) {
        _events.tryEmit(event)
    }
}