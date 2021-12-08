package pixel.cando.ui.main.doctor_profile

import com.github.terrakok.cicerone.Screen
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouterFragment

class DoctorProfileFlowFragment : FlowRouterFragment() {

    override val initialScreen: Screen
        get() = Screens.doctorProfile()

}