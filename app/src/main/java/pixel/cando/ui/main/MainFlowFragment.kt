package pixel.cando.ui.main

import com.github.terrakok.cicerone.Screen
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouterFragment

class MainFlowFragment : FlowRouterFragment() {

    override val initialScreen: Screen = Screens.home()

}