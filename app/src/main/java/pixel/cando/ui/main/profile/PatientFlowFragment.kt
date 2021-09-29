package pixel.cando.ui.main.profile

import com.github.terrakok.cicerone.Screen
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouterFragment

class PatientFlowFragment : FlowRouterFragment() {

    override val initialScreen: Screen
        get() = Screens.profile()

}