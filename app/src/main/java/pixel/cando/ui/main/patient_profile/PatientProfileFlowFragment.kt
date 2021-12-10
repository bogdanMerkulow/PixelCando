package pixel.cando.ui.main.patient_profile

import com.github.terrakok.cicerone.Screen
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouterFragment

class PatientProfileFlowFragment : FlowRouterFragment() {

    override val initialScreen: Screen
        get() = Screens.patientProfile()

}