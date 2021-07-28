package pixel.cando.ui

import androidx.fragment.app.Fragment
import com.github.terrakok.cicerone.androidx.FragmentScreen
import pixel.cando.ui.auth.AuthFlowFragment
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.home.HomeFlowFragment

object Screens {

    fun authFlow() = FragmentScreen { AuthFlowFragment() }
    fun signIn() = FragmentScreen { SignInFragment() }

    fun homeFlow() = FragmentScreen { HomeFlowFragment() }

    fun empty() = FragmentScreen { Fragment() } //only for development

}