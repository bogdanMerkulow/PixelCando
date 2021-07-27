package pixel.cando.ui._base.fragment

import android.os.Handler
import android.os.Looper
import com.github.terrakok.cicerone.*


class MainThreadRouter : BaseRouter(),
    Router {

    private val mainHandler = Handler(
        Looper.getMainLooper()
    )

    override fun navigateTo(
        screen: Screen
    ) {
        executeCommandsOnMainThread(
            Forward(screen)
        )
    }

    override fun newRootScreen(screen: Screen) {
        executeCommandsOnMainThread(
            BackTo(null),
            Replace(screen)
        )
    }

    override fun replaceScreen(screen: Screen) {
        executeCommandsOnMainThread(Replace(screen))
    }

    override fun newChain(
        vararg screens: Screen
    ) {
        val commands = screens.map {
            Forward(it)
        }
        executeCommandsOnMainThread(*commands.toTypedArray())
    }

    override fun newRootChain(
        vararg screens: Screen
    ) {
        val commands = screens.mapIndexed { index, screen ->
            if (index == 0) Replace(screen)
            else Forward(screen)
        }
        executeCommandsOnMainThread(
            BackTo(null),
            *commands.toTypedArray()
        )
    }

    override fun finishChain() {
        executeCommandsOnMainThread(
            BackTo(null),
            Back()
        )
    }

    override fun exit() {
        executeCommandsOnMainThread(Back())
    }

    private fun executeCommandsOnMainThread(
        vararg commands: Command
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            executeCommands(*commands)
        } else {
            mainHandler.post {
                executeCommands(*commands)
            }
        }
    }

}