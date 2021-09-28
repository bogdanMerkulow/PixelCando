package pixel.cando.ui

import androidx.fragment.app.Fragment
import com.github.terrakok.cicerone.androidx.FragmentScreen
import pixel.cando.ui._base.fragment.withArgumentSet
import pixel.cando.ui.auth.AuthFlowFragment
import pixel.cando.ui.auth.password_recovery.PasswordRecoveryFragment
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.main.MainFlowFragment
import pixel.cando.ui.main.home.HomeFragment
import pixel.cando.ui.main.patient_details.PatientDetailsFragment
import pixel.cando.ui.main.patient_list.PatientListFragment
import pixel.cando.ui.root.RootFragment
import pixel.cando.ui.splash.SplashFragment

object Screens {

    fun root() = FragmentScreen { RootFragment() }

    fun splash() = FragmentScreen { SplashFragment() }

    fun authFlow() = FragmentScreen { AuthFlowFragment() }
    fun signIn() = FragmentScreen { SignInFragment() }
    fun passwordRecovery(
        email: String
    ) = FragmentScreen {
        PasswordRecoveryFragment()
            .withArgumentSet(email)
    }

    fun mainFlow() = FragmentScreen { MainFlowFragment() }
    fun home() = FragmentScreen { HomeFragment() }

    fun patients() = FragmentScreen { PatientListFragment() }
    fun patientDetails(
        patientId: Long,
    ) = FragmentScreen {
        PatientDetailsFragment()
            .withArgumentSet(patientId)
    }

    fun empty() = FragmentScreen { Fragment() } //only for development

}