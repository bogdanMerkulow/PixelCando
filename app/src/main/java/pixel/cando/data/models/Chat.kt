package pixel.cando.data.models

import java.time.LocalDateTime

data class ChatItem(
    val id: Long,
    val fullName: String,
    val avatarText: String,
    val avatarBgColor: String,
    val unreadCount: Int,
    val recentMessage: ChatRecentMessage?,
)

data class ChatRecentMessage(
    val id: Long,
    val createdAt: LocalDateTime,
    val senderId: Long,
    val content: String,
)
