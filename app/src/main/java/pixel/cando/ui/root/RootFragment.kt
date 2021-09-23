package pixel.cando.ui.root

import com.github.terrakok.cicerone.Screen
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.RootRouterFragment

class RootFragment : RootRouterFragment() {

    override val initialScreen: Screen = Screens.splash()

}