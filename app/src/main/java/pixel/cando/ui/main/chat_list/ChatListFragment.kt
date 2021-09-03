package pixel.cando.ui.main.chat_list

import pixel.cando.databinding.FragmentChatListBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class ChatListFragment : ViewBindingFragment<FragmentChatListBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentChatListBinding>
        get() = FragmentChatListBinding::inflate

}