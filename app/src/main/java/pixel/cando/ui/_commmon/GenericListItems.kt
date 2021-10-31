package pixel.cando.ui._commmon

import pixel.cando.databinding.ListItemInitialLoaderBinding
import pixel.cando.databinding.ListItemMoreLoaderBinding
import pixel.cando.databinding.ListItemNoDataBinding
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.list.ListState
import pixel.cando.ui._base.list.createDifferAdapterDelegateInterfaceRestricted
import pixel.cando.ui._base.list.loadedItems

interface NoDataListPlaceholder : ListItem {
    val title: String
    val description: String
}

interface ListInitialLoader : ListItem

interface ListMoreLoader : ListItem

internal inline fun <reified C : NoDataListPlaceholder, reified P : ListItem> noDataListPlaceholder(
) = createDifferAdapterDelegateInterfaceRestricted<
        NoDataListPlaceholder,
        C,
        P,
        ListItemNoDataBinding>(
    viewBindingCreator = ListItemNoDataBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.titleLabel.text = item.title
            binding.descriptionLabel.text = item.description
        }
    },
    areItemsTheSame = { _, _ -> true },
    areContentsTheSame = { _, _ -> true }
)

internal inline fun <reified C : ListInitialLoader, reified P : ListItem> listInitialLoader(
) = createDifferAdapterDelegateInterfaceRestricted<
        ListInitialLoader,
        C,
        P,
        ListItemInitialLoaderBinding>(
    viewBindingCreator = ListItemInitialLoaderBinding::inflate,
    viewHolderBinding = {},
    areItemsTheSame = { _, _ -> true },
    areContentsTheSame = { _, _ -> true }
)

internal inline fun <reified C : ListMoreLoader, reified P : ListItem> listMoreLoader(
) = createDifferAdapterDelegateInterfaceRestricted<
        ListMoreLoader,
        C,
        P,
        ListItemMoreLoaderBinding>(
    viewBindingCreator = ListItemMoreLoaderBinding::inflate,
    viewHolderBinding = {},
    areItemsTheSame = { _, _ -> true },
    areContentsTheSame = { _, _ -> true }
)

fun <T, R> ListState<T>.toListItems(
    noDataPlaceholderProvider: () -> R,
    initialLoaderProvider: () -> R,
    moreLoaderProvider: () -> R,
    itemMapper: List<T>.() -> List<R>,
): List<R> = when (this) {
    is ListState.NotInitialized,
    is ListState.EmptyError -> emptyList()
    is ListState.Empty -> listOf(
        noDataPlaceholderProvider.invoke()
    )
    is ListState.EmptyProgress -> listOf(
        initialLoaderProvider.invoke()
    )
    else -> {
        val items: List<R> = itemMapper.invoke(
            loadedItems()
        )
        if (this is ListState.NextPageLoading) {
            items.plus(moreLoaderProvider.invoke())
        } else {
            items
        }
    }
}