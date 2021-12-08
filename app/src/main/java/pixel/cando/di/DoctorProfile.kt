package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.local.SessionWiper
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.doctor_profile.DoctorProfileDataModel
import pixel.cando.ui.main.doctor_profile.DoctorProfileEffect
import pixel.cando.ui.main.doctor_profile.DoctorProfileEvent
import pixel.cando.ui.main.doctor_profile.DoctorProfileFragment
import pixel.cando.ui.main.doctor_profile.DoctorProfileLogic
import pixel.cando.ui.main.doctor_profile.DoctorProfileViewModel
import pixel.cando.ui.main.doctor_profile.viewModel
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer


fun DoctorProfileFragment.setup(
    sessionWiper: SessionWiper,
    rootRouter: RootRouter,
    resourceProvider: ResourceProvider,
    remoteRepository: RemoteRepository,
) {
    if (delegates.isNotEmpty()) {
        return
    }
    val controllerFragmentDelegate = ControllerFragmentDelegate<
            DoctorProfileViewModel,
            DoctorProfileDataModel,
            DoctorProfileEvent,
            DoctorProfileEffect>(
        loop = Mobius.loop(
            Update<DoctorProfileDataModel, DoctorProfileEvent, DoctorProfileEffect> { model, event ->
                DoctorProfileLogic.update(
                    model,
                    event
                )
            },
            DoctorProfileLogic.effectHandler(
                sessionWiper = sessionWiper,
                rootRouter = rootRouter,
                remoteRepository = remoteRepository,
                messageDisplayer = messageDisplayer,
                resourceProvider = resourceProvider,
            )
        )
            .logger(AndroidLogger.tag("DoctorProfile")),
        initialState = {
            DoctorProfileLogic.init(it)
        },
        defaultStateProvider = {
            DoctorProfileLogic.initialModel()
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