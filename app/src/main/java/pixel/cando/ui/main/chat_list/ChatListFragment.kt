package pixel.cando.ui.main.chat_list

import android.graphics.Color
import android.os.Bundle
import pixel.cando.databinding.FragmentChatListBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class ChatListFragment : ViewBindingFragment<FragmentChatListBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentChatListBinding>
        get() = FragmentChatListBinding::inflate

    override fun onViewBindingCreated(
        viewBinding: FragmentChatListBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.root.setBackgroundColor(Color.RED)
    }

}