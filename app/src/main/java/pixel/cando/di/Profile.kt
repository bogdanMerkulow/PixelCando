package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.local.SessionWiper
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.profile.*


fun ProfileFragment.setup(
    sessionWiper: SessionWiper,
    rootRouter: RootRouter,
) {
    if (delegates.isNotEmpty()) {
        return
    }
    val controllerFragmentDelegate = ControllerFragmentDelegate<
            ProfileViewModel,
            ProfileDataModel,
            ProfileEvent,
            ProfileEffect>(
        loop = Mobius.loop(
            Update<ProfileDataModel, ProfileEvent, ProfileEffect> { model, event ->
                ProfileLogic.update(
                    model,
                    event
                )
            },
            ProfileLogic.effectHandler(
                sessionWiper = sessionWiper,
                rootRouter = rootRouter,
            )
        )
            .logger(AndroidLogger.tag("Profile")),
        initialState = {
            ProfileLogic.init(it)
        },
        defaultStateProvider = {
            ProfileLogic.initialModel()
        },
        modelMapper = {
            it.viewModel()
        },
        render = null
    )

    eventSender = controllerFragmentDelegate
    delegates = setOf(
        controllerFragmentDelegate,
    )
}