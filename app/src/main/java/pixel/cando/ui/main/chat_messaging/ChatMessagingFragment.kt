package pixel.cando.ui.main.chat_messaging

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pixel.cando.R
import pixel.cando.databinding.FragmentChatMessagingBinding
import pixel.cando.databinding.ListItemChatIncomingMessageBinding
import pixel.cando.databinding.ListItemChatOutgoingMessageBinding
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
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrInvisible
import pixel.cando.utils.diffuser.intoListDifferAdapter
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.doAfterTextChanged

class ChatMessagingFragment : ViewBindingFragment<FragmentChatMessagingBinding>(
    FragmentChatMessagingBinding::inflate
), ViewModelRender<ChatMessagingViewModel>,
    EventSenderNeeder<ChatMessagingEvent>,
    DiffuserCreator<ChatMessagingViewModel, FragmentChatMessagingBinding>,
    DiffuserProviderNeeder<ChatMessagingViewModel> {

    override var eventSender: EventSender<ChatMessagingEvent>? = null

    override var diffuserProvider: DiffuserProvider<ChatMessagingViewModel>? = null

    private val adapter by lazy {
        createDifferAdapter(
            outgoingMessageAdapterDelegate(),
            incomingMessageAdapterDelegate {
                eventSender?.sendEvent(
                    ChatMessagingEvent.MessageGetVisible(it)
                )
            },
            noDataListPlaceholder<ChatMessagingItem.NoDataPlaceholder, ChatMessagingItem>(),
            listInitialLoader<ChatMessagingItem.InitialLoader, ChatMessagingItem>(),
            listMoreLoader<ChatMessagingItem.MoreLoader, ChatMessagingItem>(),
        )
    }

    private val layoutManager by lazy {
        LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
        }
    }

    override fun createDiffuser(
        viewBinding: FragmentChatMessagingBinding
    ): Diffuser<ChatMessagingViewModel> {
        return Diffuser(
            map(
                {
                    it.listState.toListItems(
                        noDataPlaceholderProvider = {
                            ChatMessagingItem.NoDataPlaceholder(
                                title = viewBinding.context.getString(R.string.chat_messaging_no_messages_title),
                                description = viewBinding.context.getString(R.string.chat_messaging_no_messages_description),
                            )
                        },
                        initialLoaderProvider = {
                            ChatMessagingItem.InitialLoader
                        },
                        moreLoaderProvider = {
                            ChatMessagingItem.MoreLoader
                        },
                        itemMapper = {
                            map {
                                when (it) {
                                    is ChatMessageViewModel.Outgoing -> ChatMessagingItem.OutgoingMessage(
                                        it
                                    )
                                    is ChatMessageViewModel.Incoming -> ChatMessagingItem.IncomingMessage(
                                        it
                                    )
                                }
                            }
                        }
                    )
                },
                intoListDifferAdapter(adapter)
            ),
            map(
                { it.isSendButtonVisible },
                intoVisibleOrInvisible(viewBinding.sendMessageIcon)
            ),
            map(
                { it.isMessageSendingProgressVisible },
                intoVisibleOrGone(viewBinding.messageSendingProgressBar)
            ),
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentChatMessagingBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.list.setHasFixedSize(true)
        viewBinding.list.layoutManager = layoutManager
        viewBinding.list.adapter = adapter
        viewBinding.list.addLoadMoreListener {
            eventSender?.sendEvent(
                ChatMessagingEvent.LoadOldMessages
            )
        }
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(
                positionStart: Int,
                itemCount: Int
            ) {
                if (positionStart == 0) {
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisibleItemPosition == 0) {
                        viewBinding.list.smoothScrollToPosition(0)
                    }
                }
            }
        })
        viewBinding.sendMessageIcon.setOnClickListener {
            eventSender?.sendEvent(
                ChatMessagingEvent.SendMessage(
                    message = viewBinding.messageField.text.toString()
                )
            )
        }
        viewBinding.messageField.doAfterTextChanged {
            eventSender?.sendEvent(
                ChatMessagingEvent.MessageChanged(
                    message = it
                )
            )
        }
    }

    override fun renderViewModel(
        viewModel: ChatMessagingViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    fun clearMessageInput() {
        viewBinding?.messageField?.text?.clear()
    }

}

private sealed class ChatMessagingItem : ListItem {
    object InitialLoader : ChatMessagingItem(),
        ListInitialLoader

    object MoreLoader : ChatMessagingItem(),
        ListMoreLoader

    data class NoDataPlaceholder(
        override val title: String,
        override val description: String,
    ) : ChatMessagingItem(),
        NoDataListPlaceholder

    data class OutgoingMessage(
        val message: ChatMessageViewModel.Outgoing
    ) : ChatMessagingItem()

    data class IncomingMessage(
        val message: ChatMessageViewModel.Incoming
    ) : ChatMessagingItem()
}

private fun outgoingMessageAdapterDelegate(
) = createDifferAdapterDelegate<
        ChatMessagingItem.OutgoingMessage,
        ChatMessagingItem,
        ListItemChatOutgoingMessageBinding
        >(
    viewBindingCreator = ListItemChatOutgoingMessageBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.contentLabel.text = item.message.content
            binding.dateLabel.text = item.message.date
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem.message.id == newItem.message.id
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun incomingMessageAdapterDelegate(
    onGetVisible: (Long) -> Unit,
) = createDifferAdapterDelegate<
        ChatMessagingItem.IncomingMessage,
        ChatMessagingItem,
        ListItemChatIncomingMessageBinding
        >(
    viewBindingCreator = ListItemChatIncomingMessageBinding::inflate,
    viewHolderBinding = {
        onViewAttachedToWindow {
            onGetVisible(item.message.id)
        }
        bind {
            binding.contentLabel.text = item.message.content
            binding.dateLabel.text = item.message.date
            binding.senderFullNameLabel.text = item.message.senderFullName
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem.message.id == newItem.message.id
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun <T, R> ChatMessageListState<T>.toListItems(
    noDataPlaceholderProvider: () -> R,
    initialLoaderProvider: () -> R,
    moreLoaderProvider: () -> R,
    itemMapper: List<T>.() -> List<R>,
): List<R> = when (this) {
    is ChatMessageListState.NotInitialized,
    is ChatMessageListState.EmptyError -> emptyList()
    is ChatMessageListState.Empty -> listOf(
        noDataPlaceholderProvider.invoke()
    )
    is ChatMessageListState.EmptyProgress -> listOf(
        initialLoaderProvider.invoke()
    )
    else -> {
        val items: List<R> = itemMapper.invoke(
            loadedItems()
        )
        if (this is ChatMessageListState.OldMessagesLoading) {
            items.plus(moreLoaderProvider.invoke())
        } else if (this is ChatMessageListState.NewMessagesLoading) {
            listOf(moreLoaderProvider.invoke())
                .plus(items)
        } else {
            items
        }
    }
}