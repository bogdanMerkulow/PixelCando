package pixel.cando.data.models

import java.time.LocalDateTime

data class ChatMessage(
    val id: Long,
    val senderId: Long,
    val senderFullName: String,
    val createdAt: LocalDateTime,
    val content: String,
)

data class MessageListPortion(
    val totalCount: Int,
    val messages: List<ChatMessage>,
)