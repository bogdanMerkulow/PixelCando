package pixel.cando.ui.main.chat_list

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ChatListState<T> {

    class NotInitialized<T> : ChatListState<T>()
    class Empty<T> : ChatListState<T>()
    class EmptyProgress<T> : ChatListState<T>()
    class EmptyError<T> : ChatListState<T>()

    data class Data<T>(
        val pageCount: Int,
        val items: List<T>
    ) : ChatListState<T>()

    data class Refreshing<T>(
        val pageCount: Int,
        val items: List<T>
    ) : ChatListState<T>()

    data class NextPageLoading<T>(
        val pageCount: Int,
        val items: List<T>
    ) : ChatListState<T>()

    data class AllData<T>(
        val pageCount: Int,
        val items: List<T>
    ) : ChatListState<T>()
}

sealed class ChatListAction<T> {

    // ui
    class Refresh<T> : ChatListAction<T>()
    class Restart<T> : ChatListAction<T>()
    class LoadMore<T> : ChatListAction<T>()

    // model
    data class PageLoaded<T>(
        val items: List<T>
    ) : ChatListAction<T>()

    class EmptyPageLoaded<T> : ChatListAction<T>()

    data class PageLoadFailed<T>(
        val error: Throwable
    ) : ChatListAction<T>()

    class StopLoading<T> : ChatListAction<T>()

    data class ContentRefreshed<T>(
        val items: List<T>
    ) : ChatListAction<T>()

}

sealed class ChatListSideEffect {
    data class LoadPage(
        val page: Int
    ) : ChatListSideEffect()

    data class EmitError(
        val error: Throwable
    ) : ChatListSideEffect()
}

fun <T> ChatListState<T>.reduce(
    action: ChatListAction<T>
): Pair<ChatListState<T>, Set<ChatListSideEffect>> = when (this) {
    is ChatListState.NotInitialized -> {
        when (action) {
            is ChatListAction.Refresh -> {
                ChatListState.EmptyProgress<T>() to setOf(ChatListSideEffect.LoadPage(0))
            }
            else -> this to emptySet()
        }
    }
    is ChatListState.Empty -> {
        when (action) {
            is ChatListAction.Refresh, is ChatListAction.Restart -> {
                ChatListState.EmptyProgress<T>() to setOf(ChatListSideEffect.LoadPage(0))
            }
            else -> this to emptySet()
        }
    }
    is ChatListState.EmptyProgress -> {
        when (action) {
            is ChatListAction.Restart -> {
                ChatListState.EmptyProgress<T>() to setOf(ChatListSideEffect.LoadPage(0))
            }
            is ChatListAction.PageLoaded -> {
                ChatListState.Data(
                    pageCount = 1,
                    items = action.items
                ) to emptySet()
            }
            is ChatListAction.EmptyPageLoaded -> {
                ChatListState.Empty<T>() to emptySet()
            }
            is ChatListAction.PageLoadFailed -> {
                ChatListState.EmptyError<T>() to setOf(ChatListSideEffect.EmitError(action.error))
            }
            is ChatListAction.StopLoading -> {
                ChatListState.Empty<T>() to emptySet()
            }
            else -> this to emptySet()
        }
    }
    is ChatListState.EmptyError -> {
        when (action) {
            is ChatListAction.Refresh, is ChatListAction.Restart -> {
                ChatListState.EmptyProgress<T>() to setOf(ChatListSideEffect.LoadPage(0))
            }
            else -> this to emptySet()
        }
    }
    is ChatListState.Data -> {
        when (action) {
            is ChatListAction.Restart -> {
                ChatListState.EmptyProgress<T>() to setOf(
                    ChatListSideEffect.LoadPage(0)
                )
            }
            is ChatListAction.Refresh -> {
                ChatListState.Refreshing(
                    pageCount = this.pageCount,
                    items = this.items,
                ) to setOf(ChatListSideEffect.LoadPage(0))
            }
            is ChatListAction.LoadMore -> {
                ChatListState.NextPageLoading(
                    pageCount = this.pageCount,
                    items = this.items,
                ) to setOf(ChatListSideEffect.LoadPage(this.pageCount))
            }
            is ChatListAction.ContentRefreshed -> {
                if (this.items == action.items) {
                    this to emptySet()
                } else {
                    ChatListState.Data(
                        pageCount = this.pageCount,
                        items = action.items,
                    ) to emptySet()
                }
            }
            else -> this to emptySet()
        }
    }
    is ChatListState.AllData -> {
        when (action) {
            is ChatListAction.Restart -> {
                ChatListState.EmptyProgress<T>() to setOf(
                    ChatListSideEffect.LoadPage(0)
                )
            }
            is ChatListAction.Refresh -> {
                ChatListState.Refreshing(
                    pageCount = this.pageCount,
                    items = this.items
                ) to setOf(ChatListSideEffect.LoadPage(0))
            }
            is ChatListAction.ContentRefreshed -> {
                if (this.items == action.items) {
                    this to emptySet()
                } else {
                    ChatListState.AllData(
                        pageCount = this.pageCount,
                        items = action.items,
                    ) to emptySet()
                }
            }
            else -> this to emptySet()
        }
    }
    is ChatListState.Refreshing -> {
        when (action) {
            is ChatListAction.Restart -> {
                ChatListState.EmptyProgress<T>() to setOf(
                    ChatListSideEffect.LoadPage(0)
                )
            }
            is ChatListAction.EmptyPageLoaded -> {
                ChatListState.Empty<T>() to emptySet()
            }
            is ChatListAction.PageLoaded -> {
                ChatListState.Data(
                    pageCount = 1,
                    items = action.items
                ) to emptySet()
            }
            is ChatListAction.PageLoadFailed -> {
                ChatListState.Data(
                    pageCount = this.pageCount,
                    items = this.items
                ) to setOf(
                    ChatListSideEffect.EmitError(action.error)
                )
            }
            is ChatListAction.StopLoading -> {
                ChatListState.Data(
                    pageCount = this.pageCount,
                    items = this.items
                ) to emptySet()
            }
            else -> this to emptySet()
        }
    }
    is ChatListState.NextPageLoading -> {
        when (action) {
            is ChatListAction.Restart -> {
                ChatListState.EmptyProgress<T>() to setOf(
                    ChatListSideEffect.LoadPage(0)
                )
            }
            is ChatListAction.Refresh -> {
                ChatListState.Refreshing(
                    pageCount = this.pageCount,
                    items = this.items
                ) to setOf(
                    ChatListSideEffect.LoadPage(0)
                )
            }
            is ChatListAction.PageLoaded -> {
                ChatListState.Data(
                    pageCount = this.pageCount + 1,
                    items = this.items.plus(action.items)
                ) to emptySet()
            }
            is ChatListAction.EmptyPageLoaded -> {
                ChatListState.AllData(
                    pageCount = this.pageCount,
                    items = this.items
                ) to emptySet()
            }
            is ChatListAction.PageLoadFailed -> {
                ChatListState.Data(
                    pageCount = this.pageCount,
                    items = this.items
                ) to setOf(
                    ChatListSideEffect.EmitError(action.error)
                )
            }
            is ChatListAction.StopLoading -> {
                ChatListState.Data(
                    pageCount = this.pageCount,
                    items = this.items
                ) to emptySet()
            }
            else -> this to emptySet()
        }
    }

}

fun <T, R> ChatListState<T>.map(
    mapper: (T, Int, List<T>) -> R
): ChatListState<R> = when (this) {
    is ChatListState.NotInitialized -> ChatListState.NotInitialized()
    is ChatListState.Empty -> ChatListState.Empty()
    is ChatListState.EmptyProgress -> ChatListState.EmptyProgress()
    is ChatListState.EmptyError -> ChatListState.EmptyError()
    is ChatListState.Data -> ChatListState.Data(
        pageCount = pageCount,
        items = items.mapIndexed { index, item ->
            mapper(
                item,
                index,
                items
            )
        }
    )
    is ChatListState.Refreshing -> ChatListState.Refreshing(
        pageCount = pageCount,
        items = items.mapIndexed { index, item ->
            mapper(
                item,
                index,
                items
            )
        }
    )
    is ChatListState.NextPageLoading -> ChatListState.NextPageLoading(
        pageCount = pageCount,
        items = items.mapIndexed { index, item ->
            mapper(
                item,
                index,
                items
            )
        }
    )
    is ChatListState.AllData -> ChatListState.AllData(
        pageCount = pageCount,
        items = items.mapIndexed { index, item ->
            mapper(
                item,
                index,
                items
            )
        }
    )
}

fun <T> ChatListState<T>.loadedItems(): List<T> = when (this) {
    is ChatListState.NotInitialized -> emptyList()
    is ChatListState.Empty -> emptyList()
    is ChatListState.EmptyProgress -> emptyList()
    is ChatListState.EmptyError -> emptyList()
    is ChatListState.Data -> this.items
    is ChatListState.AllData -> this.items
    is ChatListState.NextPageLoading -> this.items
    is ChatListState.Refreshing -> this.items
}

val <T> ChatListState<T>.isLoading: Boolean
    get() = when (this) {
        is ChatListState.NotInitialized -> false
        is ChatListState.Empty -> false
        is ChatListState.EmptyProgress -> true
        is ChatListState.EmptyError -> false
        is ChatListState.Data -> false
        is ChatListState.AllData -> false
        is ChatListState.NextPageLoading -> true
        is ChatListState.Refreshing -> true
    }

val <T> ChatListState<T>.pageCount: Int
    get() = when (this) {
        is ChatListState.NotInitialized -> 0
        is ChatListState.Empty -> 0
        is ChatListState.EmptyProgress -> 0
        is ChatListState.EmptyError -> 0
        is ChatListState.Data -> pageCount
        is ChatListState.AllData -> pageCount
        is ChatListState.NextPageLoading -> pageCount
        is ChatListState.Refreshing -> pageCount
    }

val <T> ChatListState<T>.isRefreshing: Boolean
    get() = this is ChatListState.Refreshing

sealed class ParcelableChatListState<T : Parcelable> : Parcelable {
    @Parcelize
    class NotInitialized<T : Parcelable> : ParcelableChatListState<T>()

    @Parcelize
    class Empty<T : Parcelable> : ParcelableChatListState<T>()

    @Parcelize
    class EmptyProgress<T : Parcelable> : ParcelableChatListState<T>()

    @Parcelize
    class EmptyError<T : Parcelable> : ParcelableChatListState<T>()

    @Parcelize
    data class Data<T : Parcelable>(
        val pageCount: Int,
        val items: List<T>
    ) : ParcelableChatListState<T>()

    @Parcelize
    data class Refreshing<T : Parcelable>(
        val pageCount: Int,
        val items: List<T>
    ) : ParcelableChatListState<T>()

    @Parcelize
    data class NextPageLoading<T : Parcelable>(
        val pageCount: Int,
        val items: List<T>
    ) : ParcelableChatListState<T>()

    @Parcelize
    data class AllData<T : Parcelable>(
        val pageCount: Int,
        val items: List<T>
    ) : ParcelableChatListState<T>()
}

fun <T> ParcelableChatListState<T>.loadedItems(): List<T> where T : Parcelable =
    this.plainState.loadedItems()

val <T : Parcelable> ParcelableChatListState<T>.isLoading: Boolean
    get() = this.plainState.isLoading

val <T : Parcelable> ParcelableChatListState<T>.pageCount: Int
    get() = this.plainState.pageCount

fun <T> ParcelableChatListState<T>.reduce(
    action: ChatListAction<T>
): Pair<ParcelableChatListState<T>, Set<ChatListSideEffect>> where T : Parcelable =
    this.plainState.reduce(action).let {
        val (state, effects) = it
        state.parcelableState to effects
    }

fun <T : Parcelable, R : Parcelable> ParcelableChatListState<T>.map(
    mapper: (T, Int, List<T>) -> R
): ParcelableChatListState<R> = plainState.map(mapper).parcelableState

val <T> ChatListState<T>.parcelableState: ParcelableChatListState<T> where T : Parcelable
    get() = when (this) {
        is ChatListState.NotInitialized -> ParcelableChatListState.NotInitialized()
        is ChatListState.Empty -> ParcelableChatListState.Empty()
        is ChatListState.EmptyProgress -> ParcelableChatListState.EmptyProgress()
        is ChatListState.EmptyError -> ParcelableChatListState.EmptyError()
        is ChatListState.Data -> ParcelableChatListState.Data(
            pageCount = this.pageCount,
            items = this.items
        )
        is ChatListState.AllData -> ParcelableChatListState.AllData(
            pageCount = this.pageCount,
            items = this.items
        )
        is ChatListState.NextPageLoading -> ParcelableChatListState.NextPageLoading(
            pageCount = this.pageCount,
            items = this.items
        )
        is ChatListState.Refreshing -> ParcelableChatListState.Refreshing(
            pageCount = this.pageCount,
            items = this.items
        )
    }

val <T> ParcelableChatListState<T>.plainState: ChatListState<T> where T : Parcelable
    get() = when (this) {
        is ParcelableChatListState.NotInitialized -> ChatListState.NotInitialized()
        is ParcelableChatListState.Empty -> ChatListState.Empty()
        is ParcelableChatListState.EmptyProgress -> ChatListState.EmptyProgress()
        is ParcelableChatListState.EmptyError -> ChatListState.EmptyError()
        is ParcelableChatListState.Data -> ChatListState.Data(
            pageCount = this.pageCount,
            items = this.items
        )
        is ParcelableChatListState.AllData -> ChatListState.AllData(
            pageCount = this.pageCount,
            items = this.items
        )
        is ParcelableChatListState.NextPageLoading -> ChatListState.NextPageLoading(
            pageCount = this.pageCount,
            items = this.items
        )
        is ParcelableChatListState.Refreshing -> ChatListState.Refreshing(
            pageCount = this.pageCount,
            items = this.items
        )
    }