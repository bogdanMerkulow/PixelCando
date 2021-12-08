package pixel.cando.ui.main.chat_messaging

import pixel.cando.data.models.MessageListPortion
import pixel.cando.utils.Either
import java.time.LocalDateTime

interface ChatMessagingDataSource {

    suspend fun getChatMessages(
        offset: Int,
        count: Int,
        sinceDate: LocalDateTime?,
    ): Either<MessageListPortion, Throwable>

    suspend fun sendChatMessage(
        message: String,
    ): Either<Unit, Throwable>

    suspend fun readChatMessages(
        until: LocalDateTime,
    ): Either<Unit, Throwable>

}