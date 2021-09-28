package pixel.cando.di

import android.content.Context
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.ui.main.patient_details.*
import pixel.cando.utils.*
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate

fun setup(
    fragment: PatientDetailsFragment,
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    context: Context,
    flowRouter: FlowRouter,
) {
    if (fragment.delegates.isNotEmpty()) {
        return
    }
    val patientId = fragment.getArgument<Long>()
    val permissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> PatientDetailsEvent.CameraPermissionGranted
            is PermissionCheckerResult.Denied -> PatientDetailsEvent.CameraPermissionDenied
        }
    }
    val permissionChecker = RealPermissionChecker(
        context = context,
        resultEmitter = permissionResultEventSource
    )
    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PatientDetailsViewModel,
            PatientDetailsDataModel,
            PatientDetailsEvent,
            PatientDetailsEffect>(
        loop = Mobius.loop(
            Update<PatientDetailsDataModel, PatientDetailsEvent, PatientDetailsEffect> { model, event ->
                PatientDetailsLogic.update(
                    model,
                    event
                )
            },
            PatientDetailsLogic.effectHandler(
                photoTakerOpener = {
                    doOnGlobalMain {
                        CameraFragment.show(
                            fragment.childFragmentManager
                        )
                    }
                },
                remoteRepository = remoteRepository,
                messageDisplayer = fragment.messageDisplayer,
                resourceProvider = resourceProvider,
                permissionChecker = permissionChecker,
                flowRouter = flowRouter,
            )
        )
            .eventSources(
                permissionResultEventSource
            )
            .logger(AndroidLogger.tag("PatientDetails")),
        initialState = {
            PatientDetailsLogic.init(it)
        },
        defaultStateProvider = {
            PatientDetailsLogic.initialModel(
                patientId = patientId,
            )
        },
        modelMapper = {
            it.viewModel()
        },
        render = fragment
    )

    val diffuserFragmentDelegate = DiffuserFragmentDelegate(
        fragment
    )

    fragment.eventSender = controllerFragmentDelegate
    fragment.diffuserProvider = { diffuserFragmentDelegate.diffuser }
    fragment.delegates = setOf(
        diffuserFragmentDelegate,
        controllerFragmentDelegate,
        permissionChecker,
    )
}