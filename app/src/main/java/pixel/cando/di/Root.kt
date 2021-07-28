package pixel.cando.di

import com.spotify.mobius.EventSource
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.local.AuthStateChecker
import pixel.cando.data.local.SessionWiper
import pixel.cando.ui.*
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui._base.tea.eventSources

fun setup(
    fragment: RootFragment,
    rootRouter: RootRouter,
    authStateChecker: AuthStateChecker,
    sessionWiper: SessionWiper,
    eventSources: Array<EventSource<RootEvent>>
) {
    if (fragment.delegates.isNotEmpty()) {
        return
    }
    val controllerFragmentDelegate = ControllerFragmentDelegate<
            RootViewModel,
            RootDataModel,
            RootEvent,
            RootEffect>(
        loop = Mobius.loop(
            Update<RootDataModel, RootEvent, RootEffect> { model, event ->
                RootLogic.update(
                    model,
                    event
                )
            },
            RootLogic.effectHandler(
                rootRouter = rootRouter,
                authStateChecker = authStateChecker,
                sessionWiper = sessionWiper
            )
        )
            .eventSources(
                *eventSources
            )
            .logger(AndroidLogger.tag("Root")),
        initialState = {
            RootLogic.init(it)
        },
        defaultStateProvider = {
            RootDataModel.initial
        },
        modelMapper = {
            it.viewModel
        },
        render = null
    )

    fragment.delegates = setOf(controllerFragmentDelegate)

}