package pixel.cando.ui._base.fragment

import com.github.terrakok.cicerone.Screen

interface Router {
    fun navigateTo(screen: Screen)
    fun replaceScreen(screen: Screen)
    fun newRootScreen(screen: Screen)
    fun newChain(vararg screens: Screen)
    fun newRootChain(vararg screens: Screen)

    fun finishChain()
    fun exit()
}

interface FlowRouter : Router
interface RootRouter : Router

