package pixel.cando.ui.main.chat_messaging

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.ChatMessage
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.list.ListAction
import pixel.cando.ui._base.list.ListState
import pixel.cando.ui._base.list.ParcelableListState
import pixel.cando.ui._base.list.isLoading
import pixel.cando.ui._base.list.listStateUpdater
import pixel.cando.ui._base.list.map
import pixel.cando.ui._base.list.plainState
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui._base.tea.toFirst
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
            model.listState is ParcelableListState.NotInitialized -> {
                listUpdater.update(
                    model,
                    ChatMessagingEvent.RefreshRequest
                )
                    .toFirst(model)
            }
            model.listState.isLoading -> {
                listUpdater.update(
                    model,
                    ChatMessagingEvent.StopListLoading
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
            is ChatMessagingEvent.RefreshRequest,
            is ChatMessagingEvent.MessageListLoadSuccess,
            is ChatMessagingEvent.MessageListLoadFailure,
            is ChatMessagingEvent.LoadNextPage,
            is ChatMessagingEvent.StopListLoading -> {
                listUpdater.update(
                    model,
                    event
                )
            }
        }
    }

    private val listUpdater = listStateUpdater<
            ChatMessagingDataModel,
            ChatMessagingEvent,
            ChatMessagingEffect,
            ChatMessageDataModel>(
        listStateExtractor = { listState },
        eventMapper = {
            when (it) {
                is ChatMessagingEvent.RefreshRequest -> ListAction.Refresh()
                is ChatMessagingEvent.MessageListLoadSuccess -> {
                    if (it.messages.isNotEmpty()) ListAction.PageLoaded(it.messages)
                    else ListAction.EmptyPageLoaded()
                }
                is ChatMessagingEvent.MessageListLoadFailure -> ListAction.PageLoadFailed(it.error)
                is ChatMessagingEvent.LoadNextPage -> ListAction.LoadMore()
                is ChatMessagingEvent.StopListLoading -> ListAction.StopLoading()
                else -> null
            }
        },
        modelUpdater = { copy(listState = it) },
        loadPageEffectMapper = {
            ChatMessagingEffect.LoadPage(
                chatId = chatId,
                page = it.page,
            )
        },
        emitErrorEffectMapper = {
            ChatMessagingEffect.ShowUnexpectedError
        }
    )

    fun effectHandler(
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        remoteRepository: RemoteRepository,
        flowRouter: FlowRouter,
    ): Connectable<ChatMessagingEffect, ChatMessagingEvent> {
        val loadNextPageJob = AtomicReference<Job>()
        return CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is ChatMessagingEffect.LoadPage -> {
                    loadNextPageJob.getAndSet(
                        launch {
                            val result = remoteRepository.getChatMessages(
                                chatId = effect.chatId,
                                page = effect.page,
                                sinceDate = null,
                            )
                            result.onLeft {
                                output.accept(
                                    ChatMessagingEvent.MessageListLoadSuccess(
                                        it.map { it.dataModel() }
                                    )
                                )
                            }
                            result.onRight {
                                logError(it)
                                output.accept(
                                    ChatMessagingEvent.MessageListLoadFailure(it)
                                )
                            }
                        }
                    )?.cancel()
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
        chatId: Long,
        loggedInUserId: Long,
    ) = ChatMessagingDataModel(
        chatId = chatId,
        loggedInUserId = loggedInUserId,
        listState = ParcelableListState.NotInitialized(),
    )

}

sealed class ChatMessagingEvent {

    // ui
    object RefreshRequest : ChatMessagingEvent()

    object LoadNextPage : ChatMessagingEvent()

    // model
    class MessageListLoadSuccess(
        val messages: List<ChatMessageDataModel>,
    ) : ChatMessagingEvent()

    class MessageListLoadFailure(
        val error: Throwable,
    ) : ChatMessagingEvent()

    object StopListLoading : ChatMessagingEvent()

}

sealed class ChatMessagingEffect {

    data class LoadPage(
        val chatId: Long,
        val page: Int,
    ) : ChatMessagingEffect()

    object ShowUnexpectedError : ChatMessagingEffect()

}

@Parcelize
data class ChatMessagingDataModel(
    val chatId: Long,
    val loggedInUserId: Long,
    val listState: ParcelableListState<ChatMessageDataModel>
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
    val listState: ListState<ChatMessageViewModel>,
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
        }//.reversed()
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