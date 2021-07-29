package pixel.cando.ui.main.chat

import pixel.cando.databinding.FragmentChatBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class ChatFragment : ViewBindingFragment<FragmentChatBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentChatBinding>
        get() = FragmentChatBinding::inflate

}