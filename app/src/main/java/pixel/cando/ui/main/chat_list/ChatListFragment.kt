package pixel.cando.ui.main.chat_list

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.tabs.TabLayout
import pixel.cando.R
import pixel.cando.databinding.FragmentChatListBinding
import pixel.cando.databinding.ListItemChatBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.list.createDifferAdapter
import pixel.cando.ui._base.list.createDifferAdapterDelegate
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.ui._commmon.ListInitialLoader
import pixel.cando.ui._commmon.ListMoreLoader
import pixel.cando.ui._commmon.NoDataListPlaceholder
import pixel.cando.ui._commmon.listInitialLoader
import pixel.cando.ui._commmon.listMoreLoader
import pixel.cando.ui._commmon.noDataListPlaceholder
import pixel.cando.utils.addLoadMoreListener
import pixel.cando.utils.context
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.into
import pixel.cando.utils.diffuser.Diffuser.intoAlways
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.intoListDifferAdapter
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.visibleOrGone

class ChatListFragment : ViewBindingFragment<FragmentChatListBinding>(
    FragmentChatListBinding::inflate
), ViewModelRender<ChatListViewModel>,
    EventSenderNeeder<ChatListEvent>,
    DiffuserCreator<ChatListViewModel, FragmentChatListBinding>,
    DiffuserProviderNeeder<ChatListViewModel> {

    override var eventSender: EventSender<ChatListEvent>? = null

    override var diffuserProvider: DiffuserProvider<ChatListViewModel>? = null

    private val adapter by lazy {
        createDifferAdapter(
            chatAdapterDelegate {
                eventSender?.sendEvent(
                    ChatListEvent.PickChat(it)
                )
            },
            noDataListPlaceholder<ChatListItem.NoDataPlaceholder, ChatListItem>(),
            listInitialLoader<ChatListItem.InitialLoader, ChatListItem>(),
            listMoreLoader<ChatListItem.MoreLoader, ChatListItem>(),
        )
    }

    override fun createDiffuser(
        viewBinding: FragmentChatListBinding
    ): Diffuser<ChatListViewModel> {
        return Diffuser(
            map(
                { it.listState.isRefreshing },
                intoAlways { viewBinding.swipeRefresh.isRefreshing = it }
            ),
            map(
                { it.folders },
                into {
                    viewBinding.folderTabs.removeAllTabs()

                    it.forEach { folder ->
                        val tab = viewBinding.folderTabs.newTab()
                        tab.text = folder.title
                        tab.tag = folder.id
                        viewBinding.folderTabs.addTab(tab)
                    }
                }
            ),
            map(
                { it.pickedFolderIndex },
                into {
                    viewBinding.folderTabs.selectTab(
                        viewBinding.folderTabs.getTabAt(it)
                    )
                }
            ),
            map(
                {
                    it.listState.toListItems(
                        noDataPlaceholderProvider = {
                            ChatListItem.NoDataPlaceholder(
                                title = viewBinding.context.getString(R.string.no_patients_title),
                                description = viewBinding.context.getString(R.string.no_patients_description),
                            )
                        },
                        initialLoaderProvider = {
                            ChatListItem.InitialLoader
                        },
                        moreLoaderProvider = {
                            ChatListItem.MoreLoader
                        },
                        itemMapper = {
                            map { ChatListItem.Chat(it) }
                        }
                    )
                },
                intoListDifferAdapter(adapter)
            ),
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentChatListBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.list.setHasFixedSize(true)
        viewBinding.list.adapter = adapter
        viewBinding.list.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        viewBinding.swipeRefresh.setOnRefreshListener {
            eventSender?.sendEvent(
                ChatListEvent.RefreshRequest
            )
        }
        viewBinding.list.addLoadMoreListener {
            eventSender?.sendEvent(
                ChatListEvent.LoadNextPage
            )
        }
        viewBinding.folderTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val id = tab.tag as? Long
                        ?: return
                    eventSender?.sendEvent(
                        ChatListEvent.PickFolder(id)
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            }
        )
    }

    override fun renderViewModel(
        viewModel: ChatListViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onResume() {
        super.onResume()
        eventSender?.sendEvent(
            ChatListEvent.ScreenGotVisible
        )
    }

    override fun onPause() {
        super.onPause()
        eventSender?.sendEvent(
            ChatListEvent.ScreenGotInvisible
        )
    }

}

private sealed class ChatListItem : ListItem {
    object InitialLoader : ChatListItem(),
        ListInitialLoader

    object MoreLoader : ChatListItem(),
        ListMoreLoader

    data class NoDataPlaceholder(
        override val title: String,
        override val description: String,
    ) : ChatListItem(),
        NoDataListPlaceholder

    data class Chat(
        val chat: ChatItemViewModel
    ) : ChatListItem()
}

private fun chatAdapterDelegate(
    clickListener: (Long) -> Unit
) = createDifferAdapterDelegate<
        ChatListItem.Chat,
        ChatListItem,
        ListItemChatBinding>(
    viewBindingCreator = ListItemChatBinding::inflate,
    viewHolderBinding = {
        binding.root.setOnClickListener {
            clickListener.invoke(item.chat.id)
        }
        bind {
            binding.fullNameLabel.text = item.chat.fullName
            binding.recentMessageContentLabel.text = item.chat.recentMessageContent
            binding.avatarLabel.text = item.chat.avatarText
            binding.avatarLabel.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.chat.avatarBgColor)
            }
            binding.recentMessageDateLabel.text = item.chat.recentMessageDate
            binding.myMessageIndicator.visibleOrGone(item.chat.isRecentMessageMine ?: false)
            binding.unreadCountLabel.text = item.chat.unreadCount
            binding.unreadCountLabel.visibleOrGone(item.chat.isUnreadCountVisible)
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem.chat.id == newItem.chat.id
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun <T, R> ChatListState<T>.toListItems(
    noDataPlaceholderProvider: () -> R,
    initialLoaderProvider: () -> R,
    moreLoaderProvider: () -> R,
    itemMapper: List<T>.() -> List<R>,
): List<R> = when (this) {
    is ChatListState.NotInitialized,
    is ChatListState.EmptyError -> emptyList()
    is ChatListState.Empty -> listOf(
        noDataPlaceholderProvider.invoke()
    )
    is ChatListState.EmptyProgress -> listOf(
        initialLoaderProvider.invoke()
    )
    else -> {
        val items: List<R> = itemMapper.invoke(
            loadedItems()
        )
        if (this is ChatListState.NextPageLoading) {
            items.plus(moreLoaderProvider.invoke())
        } else {
            items
        }
    }
}