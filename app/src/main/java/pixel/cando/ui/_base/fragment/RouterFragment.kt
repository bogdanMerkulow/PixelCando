package pixel.cando.ui._base.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.Navigator
import com.github.terrakok.cicerone.Screen
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.github.terrakok.cicerone.androidx.FragmentScreen
import pixel.cando.databinding.FragmentContainerBinding

abstract class RootRouterFragment : RouterFragment(),
    RootRouter

abstract class FlowRouterFragment : RouterFragment(),
    FlowRouter

abstract class RouterFragment :
    ViewBindingFragment<FragmentContainerBinding>(),
    Router,
    OnBackPressedListener {

    private val cicerone = Cicerone.create(
        MainThreadRouter()
    )
    private val router get() = cicerone.router
    private val navigatorHolder get() = cicerone.getNavigatorHolder()
    private var navigator: Navigator? = null

    override val viewBindingCreator: ViewBindingCreator<FragmentContainerBinding>
        get() = FragmentContainerBinding::inflate

    abstract val initialScreen: Screen

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )
        viewBinding?.apply {
            navigator = object : AppNavigator(
                activity = requireActivity(),
                containerId = container.id,
                fragmentManager = childFragmentManager
            ) {
                override fun setupFragmentTransaction(
                    screen: FragmentScreen,
                    fragmentTransaction: FragmentTransaction,
                    currentFragment: Fragment?,
                    nextFragment: Fragment
                ) {
                    fragmentTransaction.setTransition(
                        FragmentTransaction.TRANSIT_FRAGMENT_FADE
                    )
                }
            }
        }
        if (childFragmentManager.fragments.isEmpty()) {
            router.replaceScreen(initialScreen)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigator = null
    }

    override fun onResume() {
        super.onResume()
        navigator?.let {
            navigatorHolder.setNavigator(it)
        }
    }

    override fun onPause() {
        navigatorHolder.removeNavigator()
        super.onPause()
    }

    override fun navigateTo(
        screen: Screen,
        clearContainer: Boolean
    ) {
        router.navigateTo(
            screen,
            clearContainer
        )
    }

    override fun newRootScreen(screen: Screen) {
        router.newRootScreen(screen)
    }

    override fun replaceScreen(screen: Screen) {
        router.replaceScreen(screen)
    }

    override fun newChain(
        vararg screens: Screen,
        clearContainer: Boolean
    ) {
        router.newChain(
            *screens,
            clearContainer = clearContainer
        )
    }

    override fun newRootChain(
        vararg screens: Screen,
        clearContainer: Boolean
    ) {
        router.newRootChain(
            *screens,
            clearContainer = clearContainer
        )
    }

    override fun finishChain() {
        router.finishChain()
    }

    override fun exit() {
        router.exit()
    }

    override fun onBackPressed() {
        val fragment = childFragmentManager.fragments.lastOrNull()
        (fragment as? OnBackPressedListener)?.onBackPressed() ?: router.exit()
    }
}