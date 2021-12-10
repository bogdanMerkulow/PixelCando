package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class ChatListRequest(
    @Json(name = "offset") val offset: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "filters") val filters: ChatListFilterDto,
)

@JsonClass(generateAdapter = true)
data class ChatListFilterDto(
    @Json(name = "query") val query: String?,
    @Json(name = "folderId") val folderId: Long?,
)

@JsonClass(generateAdapter = true)
data class ChatListResponse(
    @Json(name = "results") val results: List<ChatItemDto>
)

@JsonClass(generateAdapter = true)
data class ChatItemDto(
    @Json(name = "id") val id: Long,
    @Json(name = "fullName") val fullName: String,
    @Json(name = "avatar") val avatar: ChatParticipantAvatarDto,
    @Json(name = "recentMessage") val recentMessage: ChatRecentMessageDto?,
    @Json(name = "unreadCount") val unreadCount: Int,
)

@JsonClass(generateAdapter = true)
data class ChatParticipantAvatarDto(
    @Json(name = "color") val color: String,
    @Json(name = "abbr") val abbr: String,
)

@JsonClass(generateAdapter = true)
data class ChatRecentMessageDto(
    @Json(name = "id") val id: Long,
    @Json(name = "senderId") val senderId: Long,
    @Json(name = "createdAt") val createdAt: LocalDateTime,
    @Json(name = "content") val content: String,
)