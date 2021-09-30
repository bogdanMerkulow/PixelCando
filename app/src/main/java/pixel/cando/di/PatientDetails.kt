package pixel.cando.di

import android.content.Context
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.SimpleFragmentDelegate
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui._base.tea.ResultEventSource
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.ui.main.patient_details.PatientDetailsDataModel
import pixel.cando.ui.main.patient_details.PatientDetailsEffect
import pixel.cando.ui.main.patient_details.PatientDetailsEvent
import pixel.cando.ui.main.patient_details.PatientDetailsFragment
import pixel.cando.ui.main.patient_details.PatientDetailsLogic
import pixel.cando.ui.main.patient_details.PatientDetailsViewModel
import pixel.cando.ui.main.patient_details.viewModel
import pixel.cando.ui.main.photo_preview.PhotoPreviewFragment
import pixel.cando.ui.main.photo_preview.PhotoPreviewResult
import pixel.cando.utils.PermissionCheckerResult
import pixel.cando.utils.RealPermissionChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.createPermissionCheckerResultEventSource
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.doOnGlobalMain
import pixel.cando.utils.messageDisplayer

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

    val photoPreviewDependencies = PhotoPreviewForPatientDetailsDependencies(
        resultEmitter = ResultEventSource {
            when (it) {
                is PhotoPreviewResult.Accepted -> PatientDetailsEvent.PhotoAccepted(it.bitmap)
                is PhotoPreviewResult.Declined -> PatientDetailsEvent.PhotoDeclined
            }
        }
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
                photoConfirmationAsker = {
                    doOnGlobalMain {
                        PhotoPreviewFragment.show(
                            photo = it,
                            fragmentManager = fragment.childFragmentManager,
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
                permissionResultEventSource,
                photoPreviewDependencies.resultEmitter,
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
            it.viewModel(
                resourceProvider = resourceProvider,
            )
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
        photoPreviewDependencies,
    )
}

class PhotoPreviewForPatientDetailsDependencies(
    override val resultEmitter: ResultEventSource<PhotoPreviewResult, PatientDetailsEvent>
) : SimpleFragmentDelegate(),
    PhotoPreviewDependencies