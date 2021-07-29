package pixel.cando.ui

import androidx.fragment.app.Fragment
import com.github.terrakok.cicerone.androidx.FragmentScreen
import pixel.cando.ui.auth.AuthFlowFragment
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.main.MainFlowFragment
import pixel.cando.ui.main.home.HomeFragment

object Screens {

    fun authFlow() = FragmentScreen { AuthFlowFragment() }
    fun signIn() = FragmentScreen { SignInFragment() }

    fun mainFlow() = FragmentScreen { MainFlowFragment() }
    fun home() = FragmentScreen { HomeFragment() }

    fun empty() = FragmentScreen { Fragment() } //only for development

}