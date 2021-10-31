package pixel.cando.ui.auth

import com.github.terrakok.cicerone.Screen
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouterFragment

class AuthFlowFragment : FlowRouterFragment() {

    override val initialScreen: Screen = Screens.signIn()

}