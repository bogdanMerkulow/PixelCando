package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class ChatMessageListRequest(
    @Json(name = "userId") val userId: Long,
    @Json(name = "offset") val offset: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "filters") val filters: ChatMessageListFilterDto?,
)

@JsonClass(generateAdapter = true)
data class ChatMessageListFilterDto(
    @Json(name = "since") val since: LocalDateTime
)

@JsonClass(generateAdapter = true)
data class ChatMessageListResponse(
    @Json(name = "count") val count: Int,
    @Json(name = "results") val results: List<ChatMessageDto>,
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "id") val id: Long,
    @Json(name = "senderId") val senderId: Long,
    @Json(name = "sender") val sender: ChatMessageSenderDto,
    @Json(name = "createdAt") val createdAt: LocalDateTime,
    @Json(name = "content") val content: String,
)

@JsonClass(generateAdapter = true)
data class ChatMessageSenderDto(
    @Json(name = "fullName") val fullName: String,
)

@JsonClass(generateAdapter = true)
data class SendChatMessageRequest(
    @Json(name = "message") val message: SendChatMessageDto,
)

@JsonClass(generateAdapter = true)
data class SendChatMessageDto(
    @Json(name = "recipientId") val recipientId: Long,
    @Json(name = "content") val content: String,
)

@JsonClass(generateAdapter = true)
data class ReadChatMessagesRequest(
    @Json(name = "patientId") val patientId: Long,
    @Json(name = "until") val until: LocalDateTime,
)