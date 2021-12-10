package pixel.cando.ui.main.chat_flow

import com.github.terrakok.cicerone.Screen
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouterFragment

class ChatFlowFragment : FlowRouterFragment() {

    override val initialScreen: Screen
        get() = Screens.chatList()

}