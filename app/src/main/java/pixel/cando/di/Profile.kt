package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.local.SessionWiper
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.profile.ProfileDataModel
import pixel.cando.ui.main.profile.ProfileEffect
import pixel.cando.ui.main.profile.ProfileEvent
import pixel.cando.ui.main.profile.ProfileFragment
import pixel.cando.ui.main.profile.ProfileLogic
import pixel.cando.ui.main.profile.ProfileViewModel
import pixel.cando.ui.main.profile.viewModel
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer


fun ProfileFragment.setup(
    sessionWiper: SessionWiper,
    rootRouter: RootRouter,
    resourceProvider: ResourceProvider,
    remoteRepository: RemoteRepository,
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
                remoteRepository = remoteRepository,
                messageDisplayer = messageDisplayer,
                resourceProvider = resourceProvider,
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
            it.viewModel(
                resourceProvider = resourceProvider,
            )
        },
        render = this
    )

    val diffuserFragmentDelegate = DiffuserFragmentDelegate(
        this
    )

    eventSender = controllerFragmentDelegate
    diffuserProvider = { diffuserFragmentDelegate.diffuser }
    delegates = setOf(
        diffuserFragmentDelegate,
        controllerFragmentDelegate,
    )

}