package pixel.cando.ui.main.chat_messaging

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.ChatMessage
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

object ChatMessagingLogic {

    fun init(
        model: ChatMessagingDataModel
    ): First<ChatMessagingDataModel, ChatMessagingEffect> {
        return when {
            model.listState is ParcelableChatMessageListState.NotInitialized -> {
                model.listState.reduce(
                    ChatMessageListAction.Refresh()
                ).toFirst(model)
            }
            model.listState.isLoading -> {
                model.listState.reduce(
                    ChatMessageListAction.StopLoading()
                ).toFirst(model)
            }
            else -> {
                First.first(
                    model
                )
            }
        }
    }

    fun update(
        model: ChatMessagingDataModel,
        event: ChatMessagingEvent
    ): Next<ChatMessagingDataModel, ChatMessagingEffect> {
        return when (event) {
            is ChatMessagingEvent.RefreshRequest -> {
                model.listState.reduce(
                    ChatMessageListAction.Refresh()
                ).toNext(model)
            }
            is ChatMessagingEvent.MessagesLoadSuccess -> {
                model.listState.reduce(
                    ChatMessageListAction.MessagesLoadSuccess(
                        totalCount = event.totalCount,
                        items = event.messages,
                    )
                ).toNext(model)
            }
            is ChatMessagingEvent.MessagesLoadFailure -> {
                model.listState.reduce(
                    ChatMessageListAction.MessagesLoadFailure(
                        error = event.error
                    )
                ).toNext(model)
            }
            is ChatMessagingEvent.LoadOldMessages -> {
                model.listState.reduce(
                    ChatMessageListAction.LoadOldMessages()
                ).toNext(model)
            }
            is ChatMessagingEvent.LoadNewMessages -> {
                model.listState.reduce(
                    ChatMessageListAction.LoadNewMessages()
                ).toNext(model)
            }
            is ChatMessagingEvent.StopListLoading -> {
                model.listState.reduce(
                    ChatMessageListAction.StopLoading()
                ).toNext(model)
            }
            is ChatMessagingEvent.MessageChanged -> {
                val maySendMessage = event.message.isNotBlank()
                if (maySendMessage != model.maySendMessage) {
                    Next.next(
                        model.copy(
                            maySendMessage = maySendMessage,
                        )
                    )
                } else Next.noChange()
            }
            is ChatMessagingEvent.SendMessage -> {
                Next.next(
                    model.copy(
                        isSendingMessage = true,
                    ),
                    setOf(
                        ChatMessagingEffect.SendMessage(
                            userId = model.userId,
                            message = event.message,
                        )
                    )
                )
            }
            is ChatMessagingEvent.SendMessageSuccess -> {
                Next.next(
                    model.copy(
                        maySendMessage = false,
                        isSendingMessage = false,
                    ),
                    setOf(
                        ChatMessagingEffect.ClearMessageInput
                    )
                )
            }
            is ChatMessagingEvent.SendMessageFailure -> {
                Next.next(
                    model.copy(
                        isSendingMessage = false,
                    ),
                    setOf(
                        ChatMessagingEffect.ShowUnexpectedError
                    )
                )
            }
            is ChatMessagingEvent.MessageGetVisible -> {
                val message = model.listState.loadedItems()
                    .firstOrNull { it.id == event.messageId }
                if (message != null) {
                    Next.dispatch(
                        setOf(
                            ChatMessagingEffect.ReadChatMessages(
                                userId = model.userId,
                                until = message.createdAt,
                            )
                        )
                    )
                } else Next.noChange()
            }
            is ChatMessagingEvent.ChatMessageCountReceived -> {
                model.listState.reduce(
                    ChatMessageListAction.TotalCountChanged(
                        totalCount = event.totalCount
                    )
                ).first.reduce(
                    ChatMessageListAction.LoadNewMessages()
                ).toNext(model)
            }
            is ChatMessagingEvent.ScreenGotVisible -> {
                Next.dispatch(
                    setOf(
                        ChatMessagingEffect.StartPeriodicCheckForNewMessages(
                            userId = model.userId,
                        )
                    )
                )
            }
            is ChatMessagingEvent.ScreenGotInvisible -> {
                Next.dispatch(
                    setOf(
                        ChatMessagingEffect.StopPeriodicCheckForNewMessages
                    )
                )
            }
        }
    }

    fun effectHandler(
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        remoteRepository: RemoteRepository,
        flowRouter: FlowRouter,
        messageInputClearer: () -> Unit,
    ): Connectable<ChatMessagingEffect, ChatMessagingEvent> {
        val loadPortionJob = AtomicReference<Job>()
        val readMessagesJob = AtomicReference<Job>()
        val lastReadMessageDate = AtomicReference<LocalDateTime>()
        val periodicCheckForNewMessagesJob = AtomicReference<Job>()
        return CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is ChatMessagingEffect.LoadMessagesPortion -> {
                    loadPortionJob.getAndSet(
                        launch {
                            val result = remoteRepository.getChatMessages(
                                userId = effect.userId,
                                offset = effect.offset,
                                count = effect.count,
                                sinceDate = null,
                            )
                            result.onLeft {
                                output.accept(
                                    ChatMessagingEvent.MessagesLoadSuccess(
                                        totalCount = it.totalCount,
                                        messages = it.messages.map { it.dataModel() },
                                    )
                                )
                            }
                            result.onRight {
                                logError(it)
                                output.accept(
                                    ChatMessagingEvent.MessagesLoadFailure(it)
                                )
                            }
                        }
                    )?.cancel()
                }
                is ChatMessagingEffect.SendMessage -> {
                    val sendMessageResult = remoteRepository.sendChatMessage(
                        userId = effect.userId,
                        message = effect.message,
                    )
                    val messageListResult = remoteRepository.getChatMessages(
                        userId = effect.userId,
                        offset = 0,
                        count = 0,
                        sinceDate = null,
                    )
                    sendMessageResult.onLeft {
                        messageListResult.onLeft {
                            output.accept(
                                ChatMessagingEvent.SendMessageSuccess
                            )
                            output.accept(
                                ChatMessagingEvent.ChatMessageCountReceived(
                                    totalCount = it.totalCount
                                )
                            )
                        }
                        messageListResult.onRight {
                            logError(it)
                            output.accept(
                                ChatMessagingEvent.SendMessageFailure
                            )
                        }
                    }
                    sendMessageResult.onRight {
                        logError(it)
                        output.accept(
                            ChatMessagingEvent.SendMessageFailure
                        )
                    }
                }
                is ChatMessagingEffect.ReadChatMessages -> {
                    val previousLastReadMessageDate = lastReadMessageDate.get()
                    if ((previousLastReadMessageDate == null || previousLastReadMessageDate < effect.until)
                        && lastReadMessageDate.compareAndSet(
                            previousLastReadMessageDate,
                            effect.until
                        )
                    ) {
                        readMessagesJob.getAndSet(
                            launch {
                                delay(500)
                                val result = remoteRepository.readChatMessages(
                                    userId = effect.userId,
                                    until = effect.until,
                                )
                                result.onRight {
                                    logError(it)
                                }
                            }
                        )?.cancel()
                    }
                }
                is ChatMessagingEffect.StartPeriodicCheckForNewMessages -> {
                    periodicCheckForNewMessagesJob.getAndSet(
                        launch {
                            while (isActive) {
                                val result = remoteRepository.getChatMessages(
                                    userId = effect.userId,
                                    offset = 0,
                                    count = 0,
                                    sinceDate = null,
                                )
                                result.onLeft {
                                    output.accept(
                                        ChatMessagingEvent.ChatMessageCountReceived(
                                            totalCount = it.totalCount
                                        )
                                    )
                                }
                                result.onRight {
                                    logError(it)
                                }
                                delay(2_000L)
                            }
                        }
                    )?.cancel()
                }
                is ChatMessagingEffect.StopPeriodicCheckForNewMessages -> {
                    periodicCheckForNewMessagesJob.get()?.cancel()
                }
                is ChatMessagingEffect.ClearMessageInput -> {
                    messageInputClearer.invoke()
                }
                is ChatMessagingEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(
                            R.string.something_went_wrong
                        )
                    )
                }
            }
        }
    }

    fun initialModel(
        userId: Long,
        loggedInUserId: Long,
    ) = ChatMessagingDataModel(
        userId = userId,
        loggedInUserId = loggedInUserId,
        listState = ParcelableChatMessageListState.NotInitialized(),
        maySendMessage = false,
        isSendingMessage = false,
    )

}

sealed class ChatMessagingEvent {

    // ui

    object LoadOldMessages : ChatMessagingEvent()
    object LoadNewMessages : ChatMessagingEvent()

    data class SendMessage(
        val message: String
    ) : ChatMessagingEvent()

    data class MessageChanged(
        val message: String
    ) : ChatMessagingEvent()

    data class MessageGetVisible(
        val messageId: Long,
    ) : ChatMessagingEvent()

    object ScreenGotVisible : ChatMessagingEvent()
    object ScreenGotInvisible : ChatMessagingEvent()

    // model
    object RefreshRequest : ChatMessagingEvent()

    data class MessagesLoadSuccess(
        val totalCount: Int,
        val messages: List<ChatMessageDataModel>,
    ) : ChatMessagingEvent()

    data class MessagesLoadFailure(
        val error: Throwable,
    ) : ChatMessagingEvent()

    object StopListLoading : ChatMessagingEvent()

    object SendMessageSuccess : ChatMessagingEvent()

    object SendMessageFailure : ChatMessagingEvent()

    data class ChatMessageCountReceived(
        val totalCount: Int,
    ) : ChatMessagingEvent()

}

sealed class ChatMessagingEffect {

    data class LoadMessagesPortion(
        val userId: Long,
        val offset: Int,
        val count: Int,
    ) : ChatMessagingEffect()

    data class SendMessage(
        val userId: Long,
        val message: String,
    ) : ChatMessagingEffect()

    object ShowUnexpectedError : ChatMessagingEffect()

    object ClearMessageInput : ChatMessagingEffect()

    data class StartPeriodicCheckForNewMessages(
        val userId: Long,
    ) : ChatMessagingEffect()

    object StopPeriodicCheckForNewMessages : ChatMessagingEffect()

    data class ReadChatMessages(
        val userId: Long,
        val until: LocalDateTime,
    ) : ChatMessagingEffect()

}

@Parcelize
data class ChatMessagingDataModel(
    val userId: Long,
    val loggedInUserId: Long,
    val listState: ParcelableChatMessageListState<ChatMessageDataModel>,
    val maySendMessage: Boolean,
    val isSendingMessage: Boolean,
) : Parcelable

@Parcelize
data class ChatMessageDataModel(
    val id: Long,
    val senderId: Long,
    val senderFullName: String,
    val createdAt: LocalDateTime,
    val content: String,
) : Parcelable

data class ChatMessagingViewModel(
    val listState: ChatMessageListState<ChatMessageViewModel>,
    val isSendButtonVisible: Boolean,
    val isMessageSendingProgressVisible: Boolean,
)

sealed class ChatMessageViewModel {
    abstract val id: Long
    abstract val date: String
    abstract val content: String

    data class Outgoing(
        override val id: Long,
        override val date: String,
        override val content: String,
    ) : ChatMessageViewModel()

    data class Incoming(
        override val id: Long,
        override val date: String,
        val senderFullName: String,
        override val content: String,
    ) : ChatMessageViewModel()

}

private fun ChatMessage.dataModel(
) = ChatMessageDataModel(
    id = id,
    senderId = senderId,
    senderFullName = senderFullName,
    createdAt = createdAt,
    content = content,
)

fun ChatMessagingDataModel.viewModel(
    resourceProvider: ResourceProvider,
): ChatMessagingViewModel {
    val dateTimeFormatter = DateTimeFormatter
        .ofPattern("dd MMM, HH:mm")
        .withLocale(resourceProvider.getCurrentLocale())
    return ChatMessagingViewModel(
        listState = listState.plainState.map { message, _, _ ->
            message.viewModel(
                loggedInUserId = loggedInUserId,
                dateTimeFormatter = dateTimeFormatter,
            )
        },
        isSendButtonVisible = maySendMessage && isSendingMessage.not(),
        isMessageSendingProgressVisible = isSendingMessage,
    )
}

private fun ChatMessageDataModel.viewModel(
    loggedInUserId: Long,
    dateTimeFormatter: DateTimeFormatter,
) = if (senderId == loggedInUserId) {
    ChatMessageViewModel.Outgoing(
        id = id,
        date = dateTimeFormatter.format(createdAt),
        content = content,
    )
} else {
    ChatMessageViewModel.Incoming(
        id = id,
        date = dateTimeFormatter.format(createdAt),
        senderFullName = senderFullName,
        content = content,
    )
}

private fun Set<ChatMessageListSideEffect>.mapped(
    userId: Long,
) = map {
    when (it) {
        is ChatMessageListSideEffect.LoadPortion -> {
            ChatMessagingEffect.LoadMessagesPortion(
                userId = userId,
                offset = it.offset,
                count = it.count,
            )
        }
        is ChatMessageListSideEffect.EmitError -> {
            ChatMessagingEffect.ShowUnexpectedError
        }
    }
}.toSet()

private fun Pair<ParcelableChatMessageListState<ChatMessageDataModel>, Set<ChatMessageListSideEffect>>.toNext(
    model: ChatMessagingDataModel
) = second.mapped(
    userId = model.userId,
).let { effects ->
    if (model.listState == first) {
        Next.dispatch(effects)
    } else {
        Next.next(
            model.copy(
                listState = first,
            ),
            effects
        )
    }
}

private fun Pair<ParcelableChatMessageListState<ChatMessageDataModel>, Set<ChatMessageListSideEffect>>.toFirst(
    model: ChatMessagingDataModel
) = First.first(
    model.copy(
        listState = first,
    ),
    second.mapped(
        userId = model.userId,
    )
)