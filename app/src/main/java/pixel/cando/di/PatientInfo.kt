package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.patient_info.PatientInfoDataModel
import pixel.cando.ui.main.patient_info.PatientInfoEffect
import pixel.cando.ui.main.patient_info.PatientInfoEvent
import pixel.cando.ui.main.patient_info.PatientInfoFragment
import pixel.cando.ui.main.patient_info.PatientInfoLogic
import pixel.cando.ui.main.patient_info.PatientInfoViewModel
import pixel.cando.ui.main.patient_info.viewModel
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun PatientInfoFragment.setup(
    resourceProvider: ResourceProvider,
    remoteRepository: RemoteRepository,
    flowRouter: FlowRouter,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val patientId = getArgument<Long>()

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PatientInfoViewModel,
            PatientInfoDataModel,
            PatientInfoEvent,
            PatientInfoEffect>(
        loop = Mobius.loop(
            Update<PatientInfoDataModel, PatientInfoEvent, PatientInfoEffect> { model, event ->
                PatientInfoLogic.update(
                    model,
                    event
                )
            },
            PatientInfoLogic.effectHandler(
                resourceProvider = resourceProvider,
                messageDisplayer = messageDisplayer,
                remoteRepository = remoteRepository,
                flowRouter = flowRouter,
            )
        )
            .logger(AndroidLogger.tag("PatientInfo")),
        initialState = {
            PatientInfoLogic.init(it)
        },
        defaultStateProvider = {
            PatientInfoLogic.initialModel(
                patientId = patientId,
            )
        },
        modelMapper = {
            it.viewModel(
                resourceProvider = resourceProvider,
            )
        },
        render = this
    )

    val diffuserFragmentDelegate = DiffuserFragmentDelegate(this)

    eventSender = controllerFragmentDelegate
    diffuserProvider = { diffuserFragmentDelegate.diffuser }
    delegates = setOf(
        diffuserFragmentDelegate,
        controllerFragmentDelegate,
    )
}