package pixel.cando.ui.main.chat_list

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.ChatItem
import pixel.cando.data.models.Folder
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
import pixel.cando.ui._base.tea.mapEffects
import pixel.cando.ui._base.tea.toFirst
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

private const val ALL_FOLDER_ID = -1L

object ChatListLogic {

    fun init(
        model: ChatListDataModel
    ): First<ChatListDataModel, ChatListEffect> {
        return when {
            model.listState is ParcelableListState.NotInitialized -> {
                listUpdater.update(
                    model,
                    ChatListEvent.RefreshRequest
                )
                    .toFirst(model)
                    .mapEffects {
                        it.plus(ChatListEffect.LoadFolders)
                    }
            }
            model.listState.isLoading -> {
                listUpdater.update(
                    model,
                    ChatListEvent.StopListLoading
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
        model: ChatListDataModel,
        event: ChatListEvent
    ): Next<ChatListDataModel, ChatListEffect> {
        return when (event) {
            is ChatListEvent.RefreshRequest,
            is ChatListEvent.ChatListLoadSuccess,
            is ChatListEvent.ChatListLoadFailure,
            is ChatListEvent.LoadNextPage,
            is ChatListEvent.StopListLoading -> {
                listUpdater.update(
                    model,
                    event
                )
            }
            is ChatListEvent.FolderListLoaded -> {
                Next.next(
                    model.copy(
                        folders = event.folders,
                    )
                )
            }
            is ChatListEvent.PickFolder -> {
                if (event.folderId == model.currentFolderId)
                    return Next.noChange()
                val newModel = model.copy(
                    currentFolderId = event.folderId
                )
                listUpdater.update(
                    newModel,
                    event
                )
            }
        }
    }

    private val listUpdater = listStateUpdater<
            ChatListDataModel,
            ChatListEvent,
            ChatListEffect,
            ChatItemDataModel>(
        listStateExtractor = { listState },
        eventMapper = {
            when (it) {
                is ChatListEvent.RefreshRequest -> ListAction.Refresh()
                is ChatListEvent.PickFolder -> ListAction.Restart()
                is ChatListEvent.ChatListLoadSuccess -> {
                    if (it.chats.isNotEmpty()) ListAction.PageLoaded(it.chats)
                    else ListAction.EmptyPageLoaded()
                }
                is ChatListEvent.ChatListLoadFailure -> ListAction.PageLoadFailed(it.error)
                is ChatListEvent.LoadNextPage -> ListAction.LoadMore()
                is ChatListEvent.StopListLoading -> ListAction.StopLoading()
                else -> null
            }
        },
        modelUpdater = { copy(listState = it) },
        loadPageEffectMapper = {
            ChatListEffect.LoadPage(
                folderId = currentFolderId.takeIf { it != ALL_FOLDER_ID },
                page = it.page,
            )
        },
        emitErrorEffectMapper = {
            ChatListEffect.ShowUnexpectedError
        }
    )

    fun effectHandler(
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        remoteRepository: RemoteRepository,
        flowRouter: FlowRouter,
    ): Connectable<ChatListEffect, ChatListEvent> {
        val loadNextPageJob = AtomicReference<Job>()
        return CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is ChatListEffect.LoadPage -> {
                    loadNextPageJob.getAndSet(
                        launch {
                            val result = remoteRepository.getChats(
                                folderId = effect.folderId,
                                page = effect.page,
                            )
                            result.onLeft {
                                output.accept(
                                    ChatListEvent.ChatListLoadSuccess(
                                        it.map { it.dataModel }
                                    )
                                )
                            }
                            result.onRight {
                                logError(it)
                                output.accept(
                                    ChatListEvent.ChatListLoadFailure(it)
                                )
                            }
                        }
                    )?.cancel()
                }
                is ChatListEffect.LoadFolders -> {
                    val result = remoteRepository.getFolders()
                    result.onLeft {
                        output.accept(
                            ChatListEvent.FolderListLoaded(
                                it.map { it.dataModel }
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                    }
                }
                is ChatListEffect.ShowUnexpectedError -> {
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
        loggedInUserId: Long
    ) = ChatListDataModel(
        loggedInUserId = loggedInUserId,
        currentFolderId = ALL_FOLDER_ID,
        folders = emptyList(),
        listState = ParcelableListState.NotInitialized(),
    )

}

sealed class ChatListEvent {

    object RefreshRequest : ChatListEvent()

    object LoadNextPage : ChatListEvent()

    data class PickFolder(
        val folderId: Long,
    ) : ChatListEvent()

    // model
    class ChatListLoadSuccess(
        val chats: List<ChatItemDataModel>,
    ) : ChatListEvent()

    class ChatListLoadFailure(
        val error: Throwable,
    ) : ChatListEvent()

    object StopListLoading : ChatListEvent()

    data class FolderListLoaded(
        val folders: List<FolderDataModel>
    ) : ChatListEvent()

}

sealed class ChatListEffect {

    data class LoadPage(
        val folderId: Long?,
        val page: Int,
    ) : ChatListEffect()

    object LoadFolders : ChatListEffect()

    object ShowUnexpectedError : ChatListEffect()

}

@Parcelize
data class ChatListDataModel(
    val loggedInUserId: Long,
    val currentFolderId: Long,
    val folders: List<FolderDataModel>,
    val listState: ParcelableListState<ChatItemDataModel>,
) : Parcelable

@Parcelize
data class FolderDataModel(
    val id: Long,
    val title: String,
) : Parcelable

@Parcelize
data class ChatItemDataModel(
    val id: Long,
    val fullName: String,
    val avatarText: String,
    val avatarBgColor: String,
    val unreadCount: Int,
    val recentMessage: ChatRecentMessageDataModel?,
) : Parcelable

@Parcelize
data class ChatRecentMessageDataModel(
    val id: Long,
    val createdAt: LocalDateTime,
    val content: String,
    val senderId: Long,
) : Parcelable

data class ChatListViewModel(
    val folders: List<FolderViewModel>,
    val pickedFolderIndex: Int,
    val listState: ListState<ChatItemViewModel>
)

data class FolderViewModel(
    val id: Long,
    val title: String,
)

data class ChatItemViewModel(
    val id: Long,
    val fullName: String,
    val avatarText: String,
    @ColorInt val avatarBgColor: Int,
    val unreadCount: String,
    val isUnreadCountVisible: Boolean,
    val recentMessageContent: String?,
    val recentMessageDate: String?,
    val isRecentMessageMine: Boolean?,
)

fun ChatListDataModel.viewModel(
    resourceProvider: ResourceProvider
): ChatListViewModel {
    val foldersToShow = listOf(
        FolderViewModel(
            id = ALL_FOLDER_ID,
            title = resourceProvider.getString(R.string.all)
        )
    ) + folders.map {
        it.viewModel
    }
    val todayDateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withLocale(resourceProvider.getCurrentLocale())
    val otherDateTimeFormatter = DateTimeFormatter
        .ofPattern("dd MMM")
        .withLocale(resourceProvider.getCurrentLocale())
    return ChatListViewModel(
        folders = foldersToShow,
        pickedFolderIndex = foldersToShow.indexOfFirst { it.id == currentFolderId },
        listState = listState.plainState.map { chat, _, _ ->
            chat.viewModel(
                currentUserId = loggedInUserId,
                resourceProvider = resourceProvider,
                todayDateTimeFormatter = todayDateTimeFormatter,
                otherDateTimeFormatter = otherDateTimeFormatter,
            )
        }
    )
}

private val FolderDataModel.viewModel: FolderViewModel
    get() = FolderViewModel(
        id = id,
        title = title,
    )

private fun ChatItemDataModel.viewModel(
    currentUserId: Long,
    resourceProvider: ResourceProvider,
    todayDateTimeFormatter: DateTimeFormatter,
    otherDateTimeFormatter: DateTimeFormatter,
) = ChatItemViewModel(
    id = id,
    fullName = fullName,
    avatarText = avatarText,
    avatarBgColor = try {
        avatarBgColor.toColorInt()
    } catch (ex: IllegalArgumentException) {
        resourceProvider.getColor(R.color.blue_boston)
    },
    unreadCount = unreadCount.toString(),
    isUnreadCountVisible = unreadCount != 0,
    recentMessageContent = recentMessage?.content,
    recentMessageDate = recentMessage?.createdAt?.let {
        if (it.toLocalDate() == LocalDate.now()) todayDateTimeFormatter.format(it)
        otherDateTimeFormatter.format(it)
    },
    isRecentMessageMine = recentMessage?.senderId?.let { currentUserId == it }
)

private val Folder.dataModel: FolderDataModel
    get() = FolderDataModel(
        id = id,
        title = title,
    )

private val ChatItem.dataModel: ChatItemDataModel
    get() = ChatItemDataModel(
        id = id,
        fullName = fullName,
        avatarText = avatarText,
        avatarBgColor = avatarBgColor,
        unreadCount = unreadCount,
        recentMessage = recentMessage?.let {
            ChatRecentMessageDataModel(
                id = it.id,
                createdAt = it.createdAt,
                senderId = it.senderId,
                content = it.content,
            )
        }
    )