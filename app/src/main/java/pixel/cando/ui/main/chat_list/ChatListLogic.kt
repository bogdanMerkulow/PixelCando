package pixel.cando.ui.main.chat_list

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.ChatItem
import pixel.cando.data.models.Folder
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui._base.tea.mapEffects
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
            model.listState is ParcelableChatListState.NotInitialized -> {
                model.listState.reduce(
                    ChatListAction.Refresh()
                ).toFirst(model)
                    .mapEffects {
                        it.plus(ChatListEffect.LoadFolders)
                    }
            }
            model.listState.isLoading -> {
                model.listState.reduce(
                    ChatListAction.StopLoading()
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
            is ChatListEvent.RefreshRequest -> {
                model.listState.reduce(
                    ChatListAction.Refresh()
                ).toNext(model)
            }
            is ChatListEvent.ChatListLoadSuccess -> {
                val next = model.listState.reduce(
                    if (event.chats.isNotEmpty()) ChatListAction.PageLoaded(event.chats)
                    else ChatListAction.EmptyPageLoaded()
                ).toNext(model)

                next.mapEffects {
                    it.plus(
                        ChatListEffect.StartPeriodicRefresh(
                            folderId = model.currentFolderId.ignoreAllFolder(),
                            pageCount = next.modelOrElse(model).listState.pageCount,
                        )
                    )
                }
            }
            is ChatListEvent.ChatListLoadFailure -> {
                model.listState.reduce(
                    ChatListAction.PageLoadFailed(event.error)
                ).toNext(model)
            }
            is ChatListEvent.LoadNextPage -> {
                model.listState.reduce(
                    ChatListAction.LoadMore()
                ).toNext(model)
            }
            is ChatListEvent.StopListLoading -> {
                model.listState.reduce(
                    ChatListAction.StopLoading()
                ).toNext(model)
            }
            is ChatListEvent.ChatListRefreshed -> {
                model.listState.reduce(
                    ChatListAction.ContentRefreshed(
                        items = event.chats,
                    )
                ).toNext(model)
            }
            is ChatListEvent.FolderListLoaded -> {
                Next.next(
                    model.copy(
                        folders = event.folders,
                    )
                )
            }
            is ChatListEvent.PickFolder -> {
                if (event.folderId != model.currentFolderId) {
                    val newModel = model.copy(
                        currentFolderId = event.folderId
                    )
                    newModel.listState.reduce(
                        ChatListAction.Restart()
                    ).toNext(newModel)
                } else Next.noChange()
            }
            is ChatListEvent.PickChat -> {
                Next.dispatch(
                    setOf(
                        ChatListEffect.NavigateToChat(
                            userId = event.chatId,
                        )
                    )
                )
            }
            is ChatListEvent.ScreenGotVisible -> {
                val pageCount = model.listState.pageCount
                if (pageCount != 0) {
                    Next.dispatch(
                        setOf(
                            ChatListEffect.StartPeriodicRefresh(
                                folderId = model.currentFolderId.ignoreAllFolder(),
                                pageCount = model.listState.pageCount,
                            )
                        )
                    )
                } else Next.noChange()
            }
            is ChatListEvent.ScreenGotInvisible -> {
                Next.dispatch(
                    setOf(
                        ChatListEffect.StopPeriodicRefresh
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
    ): Connectable<ChatListEffect, ChatListEvent> {
        val loadNextPageJob = AtomicReference<Job>()
        val refreshChatsJob = AtomicReference<Job>()
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
                is ChatListEffect.StartPeriodicRefresh -> {
                    refreshChatsJob.getAndSet(
                        launch {
                            while (isActive) {
                                val result = remoteRepository.getChatsForPages(
                                    folderId = effect.folderId,
                                    pageCount = effect.pageCount,
                                )
                                result.onLeft {
                                    output.accept(
                                        ChatListEvent.ChatListRefreshed(
                                            chats = it.map { it.dataModel }
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
                is ChatListEffect.StopPeriodicRefresh -> {
                    refreshChatsJob.get()?.cancel()
                }
                is ChatListEffect.NavigateToChat -> {
                    flowRouter.navigateTo(
                        Screens.chatMessaging(
                            userId = effect.userId,
                        )
                    )
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
        listState = ParcelableChatListState.NotInitialized(),
    )

}

sealed class ChatListEvent {

    // ui
    object RefreshRequest : ChatListEvent()

    object LoadNextPage : ChatListEvent()

    data class PickFolder(
        val folderId: Long,
    ) : ChatListEvent()

    data class PickChat(
        val chatId: Long,
    ) : ChatListEvent()

    object ScreenGotVisible : ChatListEvent()
    object ScreenGotInvisible : ChatListEvent()

    // model
    data class ChatListLoadSuccess(
        val chats: List<ChatItemDataModel>,
    ) : ChatListEvent()

    data class ChatListLoadFailure(
        val error: Throwable,
    ) : ChatListEvent()

    object StopListLoading : ChatListEvent()

    data class FolderListLoaded(
        val folders: List<FolderDataModel>
    ) : ChatListEvent()

    data class ChatListRefreshed(
        val chats: List<ChatItemDataModel>,
    ) : ChatListEvent()

}

sealed class ChatListEffect {

    data class LoadPage(
        val folderId: Long?,
        val page: Int,
    ) : ChatListEffect()

    object LoadFolders : ChatListEffect()

    data class NavigateToChat(
        val userId: Long,
    ) : ChatListEffect()

    object ShowUnexpectedError : ChatListEffect()

    data class StartPeriodicRefresh(
        val folderId: Long?,
        val pageCount: Int,
    ) : ChatListEffect()

    object StopPeriodicRefresh : ChatListEffect()

}

@Parcelize
data class ChatListDataModel(
    val loggedInUserId: Long,
    val currentFolderId: Long,
    val folders: List<FolderDataModel>,
    val listState: ParcelableChatListState<ChatItemDataModel>,
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
    val listState: ChatListState<ChatItemViewModel>
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

private fun Set<ChatListSideEffect>.mapped(
    folderId: Long?
) = map {
    when (it) {
        is ChatListSideEffect.LoadPage -> {
            ChatListEffect.LoadPage(
                folderId = folderId,
                page = it.page,
            )
        }
        is ChatListSideEffect.EmitError -> {
            ChatListEffect.ShowUnexpectedError
        }
    }
}.toSet()

private fun Pair<ParcelableChatListState<ChatItemDataModel>, Set<ChatListSideEffect>>.toNext(
    model: ChatListDataModel
) = second.mapped(
    folderId = model.currentFolderId.ignoreAllFolder(),
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

private fun Pair<ParcelableChatListState<ChatItemDataModel>, Set<ChatListSideEffect>>.toFirst(
    model: ChatListDataModel
) = First.first(
    model.copy(
        listState = first,
    ),
    second.mapped(
        folderId = model.currentFolderId.ignoreAllFolder(),
    )
)

private fun Long.ignoreAllFolder() = takeIf { it != ALL_FOLDER_ID }