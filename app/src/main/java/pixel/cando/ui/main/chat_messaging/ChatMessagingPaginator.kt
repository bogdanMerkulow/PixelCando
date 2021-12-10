package pixel.cando.ui.main.chat_messaging

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ChatMessageListState<T> {

    class NotInitialized<T> : ChatMessageListState<T>()
    class Empty<T> : ChatMessageListState<T>()
    class EmptyProgress<T> : ChatMessageListState<T>()
    class EmptyError<T> : ChatMessageListState<T>()

    data class Data<T>(
        val totalCount: Int,
        val currentOffset: Int,
        val items: List<T>
    ) : ChatMessageListState<T>()

    data class OldMessagesLoading<T>(
        val totalCount: Int,
        val currentOffset: Int,
        val items: List<T>
    ) : ChatMessageListState<T>()

    data class NewMessagesLoading<T>(
        val totalCount: Int,
        val currentOffset: Int,
        val items: List<T>
    ) : ChatMessageListState<T>()

}

sealed class ChatMessageListAction<T> {

    // ui
    class Refresh<T> : ChatMessageListAction<T>()
    class LoadOldMessages<T> : ChatMessageListAction<T>()
    class LoadNewMessages<T> : ChatMessageListAction<T>()

    // model
    data class MessagesLoadSuccess<T>(
        val totalCount: Int,
        val items: List<T>
    ) : ChatMessageListAction<T>()

    data class MessagesLoadFailure<T>(
        val error: Throwable
    ) : ChatMessageListAction<T>()

    data class TotalCountChanged<T>(
        val totalCount: Int,
    ) : ChatMessageListAction<T>()

    class StopLoading<T> : ChatMessageListAction<T>()

}

sealed class ChatMessageListSideEffect {
    data class LoadPortion(
        val offset: Int,
        val count: Int,
    ) : ChatMessageListSideEffect()

    data class EmitError(
        val error: Throwable
    ) : ChatMessageListSideEffect()
}

private const val portionSize = 20

fun <T> ChatMessageListState<T>.reduce(
    action: ChatMessageListAction<T>
): Pair<ChatMessageListState<T>, Set<ChatMessageListSideEffect>> = when (this) {
    is ChatMessageListState.NotInitialized -> {
        when (action) {
            is ChatMessageListAction.Refresh -> {
                ChatMessageListState.EmptyProgress<T>() to setOf(
                    ChatMessageListSideEffect.LoadPortion(
                        offset = 0,
                        count = portionSize,
                    )
                )
            }
            else -> this to emptySet()
        }
    }
    is ChatMessageListState.Empty -> {
        when (action) {
            is ChatMessageListAction.Refresh -> {
                ChatMessageListState.EmptyProgress<T>() to setOf(
                    ChatMessageListSideEffect.LoadPortion(
                        offset = 0,
                        count = portionSize,
                    )
                )
            }
            is ChatMessageListAction.TotalCountChanged -> {
                if (action.totalCount != 0) {
                    ChatMessageListState.EmptyProgress<T>() to setOf(
                        ChatMessageListSideEffect.LoadPortion(
                            offset = 0,
                            count = portionSize,
                        )
                    )
                } else this to emptySet()
            }
            else -> this to emptySet()
        }
    }
    is ChatMessageListState.EmptyProgress -> {
        when (action) {
            is ChatMessageListAction.MessagesLoadSuccess -> {
                if (action.items.isEmpty()) {
                    ChatMessageListState.Empty<T>() to emptySet()
                } else {
                    ChatMessageListState.Data(
                        totalCount = action.totalCount,
                        currentOffset = 0,
                        items = action.items
                    ) to emptySet()
                }
            }
            is ChatMessageListAction.MessagesLoadFailure -> {
                ChatMessageListState.EmptyError<T>() to setOf(
                    ChatMessageListSideEffect.EmitError(action.error)
                )
            }
            is ChatMessageListAction.StopLoading -> {
                ChatMessageListState.Empty<T>() to emptySet()
            }
            else -> this to emptySet()
        }
    }
    is ChatMessageListState.EmptyError -> {
        when (action) {
            is ChatMessageListAction.Refresh -> {
                ChatMessageListState.EmptyProgress<T>() to setOf(
                    ChatMessageListSideEffect.LoadPortion(
                        offset = 0,
                        count = portionSize,
                    )
                )
            }
            else -> this to emptySet()
        }
    }
    is ChatMessageListState.Data -> {
        when (action) {
            is ChatMessageListAction.LoadOldMessages -> {
                if (this.currentOffset + this.items.size != this.totalCount - 1) {
                    ChatMessageListState.OldMessagesLoading(
                        totalCount = this.totalCount,
                        currentOffset = this.currentOffset,
                        items = this.items
                    ) to setOf(
                        ChatMessageListSideEffect.LoadPortion(
                            offset = currentOffset + this.items.size,
                            count = portionSize,
                        )
                    )
                } else this to emptySet()
            }
            is ChatMessageListAction.LoadNewMessages -> {
                if (this.currentOffset != 0) {
                    val offset = this.currentOffset - portionSize
                    ChatMessageListState.NewMessagesLoading(
                        totalCount = this.totalCount,
                        currentOffset = this.currentOffset,
                        items = this.items
                    ) to setOf(
                        ChatMessageListSideEffect.LoadPortion(
                            offset = if (offset < 0) 0 else offset,
                            count = if (offset < 0) portionSize + offset else portionSize,
                        )
                    )
                } else this to emptySet()
            }
            is ChatMessageListAction.TotalCountChanged -> {
                if (action.totalCount == this.totalCount) {
                    this to emptySet()
                } else if (this.totalCount < action.totalCount) {
                    val diff = action.totalCount - this.totalCount
                    val newOffset = currentOffset + diff
                    ChatMessageListState.NewMessagesLoading(
                        totalCount = action.totalCount,
                        currentOffset = newOffset,
                        items = this.items,
                    ) to setOf(
                        ChatMessageListSideEffect.LoadPortion(
                            offset = 0,
                            count = diff,
                        )
                    )
                } else {
                    ChatMessageListState.EmptyProgress<T>() to setOf(
                        ChatMessageListSideEffect.LoadPortion(
                            offset = 0,
                            count = portionSize,
                        )
                    )
                }
            }
            else -> this to emptySet()
        }
    }
    is ChatMessageListState.OldMessagesLoading -> {
        when (action) {
            is ChatMessageListAction.MessagesLoadSuccess -> {
                if (this.totalCount != action.totalCount) {

                    val diff = action.totalCount - this.totalCount
                    val newOffset = currentOffset - diff
                    ChatMessageListState.OldMessagesLoading(
                        totalCount = action.totalCount,
                        currentOffset = newOffset,
                        items = this.items,
                    ) to setOf(
                        ChatMessageListSideEffect.LoadPortion(
                            offset = newOffset + this.items.size,
                            count = portionSize,
                        )
                    )
                } else {
                    ChatMessageListState.Data(
                        totalCount = action.totalCount,
                        currentOffset = this.currentOffset,
                        items = this.items.plus(action.items),
                    ) to emptySet()
                }
            }
            is ChatMessageListAction.MessagesLoadFailure -> {
                ChatMessageListState.Data(
                    totalCount = this.totalCount,
                    currentOffset = this.currentOffset,
                    items = this.items
                ) to setOf(ChatMessageListSideEffect.EmitError(action.error))
            }
            is ChatMessageListAction.StopLoading -> {
                ChatMessageListState.Data(
                    totalCount = this.totalCount,
                    currentOffset = this.currentOffset,
                    items = this.items
                ) to emptySet()
            }
            else -> this to emptySet()
        }
    }
    is ChatMessageListState.NewMessagesLoading -> {
        when (action) {
            is ChatMessageListAction.MessagesLoadSuccess -> {
                ChatMessageListState.Data(
                    totalCount = action.totalCount,
                    currentOffset = this.currentOffset - action.items.size,
                    items = action.items.plus(this.items),
                ) to emptySet()
            }
            is ChatMessageListAction.MessagesLoadFailure -> {
                ChatMessageListState.Data(
                    totalCount = this.totalCount,
                    currentOffset = this.currentOffset,
                    items = this.items
                ) to setOf(ChatMessageListSideEffect.EmitError(action.error))
            }
            is ChatMessageListAction.StopLoading -> {
                ChatMessageListState.Data(
                    totalCount = this.totalCount,
                    currentOffset = this.currentOffset,
                    items = this.items
                ) to emptySet()
            }
            else -> this to emptySet()
        }
    }
}

fun <T, R> ChatMessageListState<T>.map(
    mapper: (T, Int, List<T>) -> R
): ChatMessageListState<R> = when (this) {
    is ChatMessageListState.NotInitialized -> ChatMessageListState.NotInitialized()
    is ChatMessageListState.Empty -> ChatMessageListState.Empty()
    is ChatMessageListState.EmptyProgress -> ChatMessageListState.EmptyProgress()
    is ChatMessageListState.EmptyError -> ChatMessageListState.EmptyError()
    is ChatMessageListState.Data -> ChatMessageListState.Data(
        totalCount = totalCount,
        currentOffset = currentOffset,
        items = items.mapIndexed { index, item ->
            mapper(
                item,
                index,
                items
            )
        }
    )
    is ChatMessageListState.OldMessagesLoading -> ChatMessageListState.OldMessagesLoading(
        totalCount = totalCount,
        currentOffset = currentOffset,
        items = items.mapIndexed { index, item ->
            mapper(
                item,
                index,
                items
            )
        }
    )
    is ChatMessageListState.NewMessagesLoading -> ChatMessageListState.NewMessagesLoading(
        totalCount = totalCount,
        currentOffset = currentOffset,
        items = items.mapIndexed { index, item ->
            mapper(
                item,
                index,
                items
            )
        }
    )
}

fun <T> ChatMessageListState<T>.loadedItems(): List<T> = when (this) {
    is ChatMessageListState.NotInitialized -> emptyList()
    is ChatMessageListState.Empty -> emptyList()
    is ChatMessageListState.EmptyProgress -> emptyList()
    is ChatMessageListState.EmptyError -> emptyList()
    is ChatMessageListState.Data -> this.items
    is ChatMessageListState.OldMessagesLoading -> this.items
    is ChatMessageListState.NewMessagesLoading -> this.items
}

val <T> ChatMessageListState<T>.isLoading: Boolean
    get() = when (this) {
        is ChatMessageListState.NotInitialized -> false
        is ChatMessageListState.Empty -> false
        is ChatMessageListState.EmptyProgress -> true
        is ChatMessageListState.EmptyError -> false
        is ChatMessageListState.Data -> false
        is ChatMessageListState.OldMessagesLoading -> true
        is ChatMessageListState.NewMessagesLoading -> true
    }


sealed class ParcelableChatMessageListState<T : Parcelable> : Parcelable {
    @Parcelize
    class NotInitialized<T : Parcelable> : ParcelableChatMessageListState<T>()

    @Parcelize
    class Empty<T : Parcelable> : ParcelableChatMessageListState<T>()

    @Parcelize
    class EmptyProgress<T : Parcelable> : ParcelableChatMessageListState<T>()

    @Parcelize
    class EmptyError<T : Parcelable> : ParcelableChatMessageListState<T>()

    @Parcelize
    data class Data<T : Parcelable>(
        val totalCount: Int,
        val currentOffset: Int,
        val items: List<T>
    ) : ParcelableChatMessageListState<T>()

    @Parcelize
    data class OldMessagesLoading<T : Parcelable>(
        val totalCount: Int,
        val currentOffset: Int,
        val items: List<T>
    ) : ParcelableChatMessageListState<T>()

    @Parcelize
    data class NewMessagesLoading<T : Parcelable>(
        val totalCount: Int,
        val currentOffset: Int,
        val items: List<T>
    ) : ParcelableChatMessageListState<T>()

}

fun <T> ParcelableChatMessageListState<T>.loadedItems(): List<T> where T : Parcelable =
    this.plainState.loadedItems()

val <T : Parcelable> ParcelableChatMessageListState<T>.isLoading: Boolean
    get() = this.plainState.isLoading

fun <T> ParcelableChatMessageListState<T>.reduce(
    action: ChatMessageListAction<T>
): Pair<ParcelableChatMessageListState<T>, Set<ChatMessageListSideEffect>> where T : Parcelable =
    this.plainState.reduce(action).let {
        val (state, effects) = it
        state.parcelableState to effects
    }

fun <T : Parcelable, R : Parcelable> ParcelableChatMessageListState<T>.map(
    mapper: (T, Int, List<T>) -> R
): ParcelableChatMessageListState<R> = plainState.map(mapper).parcelableState

val <T> ChatMessageListState<T>.parcelableState: ParcelableChatMessageListState<T> where T : Parcelable
    get() = when (this) {
        is ChatMessageListState.NotInitialized -> ParcelableChatMessageListState.NotInitialized()
        is ChatMessageListState.Empty -> ParcelableChatMessageListState.Empty()
        is ChatMessageListState.EmptyProgress -> ParcelableChatMessageListState.EmptyProgress()
        is ChatMessageListState.EmptyError -> ParcelableChatMessageListState.EmptyError()
        is ChatMessageListState.Data -> ParcelableChatMessageListState.Data(
            totalCount = this.totalCount,
            currentOffset = this.currentOffset,
            items = this.items
        )
        is ChatMessageListState.OldMessagesLoading -> ParcelableChatMessageListState.OldMessagesLoading(
            totalCount = this.totalCount,
            currentOffset = this.currentOffset,
            items = this.items
        )
        is ChatMessageListState.NewMessagesLoading -> ParcelableChatMessageListState.NewMessagesLoading(
            totalCount = this.totalCount,
            currentOffset = this.currentOffset,
            items = this.items
        )
    }

val <T> ParcelableChatMessageListState<T>.plainState: ChatMessageListState<T> where T : Parcelable
    get() = when (this) {
        is ParcelableChatMessageListState.NotInitialized -> ChatMessageListState.NotInitialized()
        is ParcelableChatMessageListState.Empty -> ChatMessageListState.Empty()
        is ParcelableChatMessageListState.EmptyProgress -> ChatMessageListState.EmptyProgress()
        is ParcelableChatMessageListState.EmptyError -> ChatMessageListState.EmptyError()
        is ParcelableChatMessageListState.Data -> ChatMessageListState.Data(
            totalCount = this.totalCount,
            currentOffset = this.currentOffset,
            items = this.items
        )
        is ParcelableChatMessageListState.OldMessagesLoading -> ChatMessageListState.OldMessagesLoading(
            totalCount = this.totalCount,
            currentOffset = this.currentOffset,
            items = this.items
        )
        is ParcelableChatMessageListState.NewMessagesLoading -> ChatMessageListState.NewMessagesLoading(
            totalCount = this.totalCount,
            currentOffset = this.currentOffset,
            items = this.items
        )
    }