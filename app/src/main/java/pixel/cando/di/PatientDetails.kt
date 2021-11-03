package pixel.cando.di

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.R
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.SimpleFragmentDelegate
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.fragment.withArgumentSet
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
import pixel.cando.utils.RealImagePicker
import pixel.cando.utils.RealPermissionChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.createImagePickerResultEventSource
import pixel.cando.utils.createPermissionCheckerResultEventSource
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun PatientDetailsFragment.setup(
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    context: Context,
    flowRouter: FlowRouter,
) {
    if (delegates.isNotEmpty()) {
        return
    }
    val patientId = getArgument<Long>()

    val cameraPermissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> PatientDetailsEvent.CameraPermissionGranted
            is PermissionCheckerResult.Denied -> PatientDetailsEvent.CameraPermissionDenied
        }
    }
    val cameraPermissionChecker = RealPermissionChecker(
        permission = Manifest.permission.CAMERA,
        context = context,
        resultEmitter = cameraPermissionResultEventSource
    )

    val writeStoragePermissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> PatientDetailsEvent.WriteStoragePermissionGranted
            is PermissionCheckerResult.Denied -> PatientDetailsEvent.WriteStoragePermissionDenied
        }
    }
    val writeStoragePermissionChecker = RealPermissionChecker(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        context = context,
        resultEmitter = writeStoragePermissionResultEventSource
    )

    val imagePickerResultEventSource = createImagePickerResultEventSource<PatientDetailsEvent> {
        PatientDetailsEvent.ImagePicked(it.uri)
    }

    val imagePicker = RealImagePicker(
        resultEmitter = imagePickerResultEventSource,
    )

    val photoPreviewDependencies = PhotoPreviewForPatientDetailsDependencies(
        resultEmitter = ResultEventSource {
            PatientDetailsEvent.PhotoAccepted(
                uri = it.uri,
                weight = it.weight,
                height = it.height,
            )
        }
    )

    val howToGetPhotoResultEventSource = ResultEventSource<Int, PatientDetailsEvent> {
        when (it) {
            0 -> PatientDetailsEvent.PhotoTakingChosen
            1 -> PatientDetailsEvent.ImagePickingChosen
            else -> error("Illegal index received")
        }
    }

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
                    lifecycleScope.launch {
                        CameraFragment.show(
                            childFragmentManager
                        )
                    }
                },
                photoConfirmationAsker = {
                    lifecycleScope.launch {
                        PhotoPreviewFragment()
                            .withArgumentSet(
                                PhotoPreviewArguments(
                                    uri = it.uri,
                                    weight = it.weight,
                                    height = it.height,
                                )
                            )
                            .show(
                                childFragmentManager,
                                ""
                            )
                    }
                },
                howToGetPhotoAsker = {
                    lifecycleScope.launch {
                        AlertDialog.Builder(requireContext())
                            .setItems(
                                arrayOf(
                                    resourceProvider.getString(R.string.camera),
                                    resourceProvider.getString(R.string.gallery),
                                )
                            ) { _, index ->
                                howToGetPhotoResultEventSource.emit(index)
                            }
                            .create()
                            .show()
                    }
                },
                remoteRepository = remoteRepository,
                messageDisplayer = messageDisplayer,
                resourceProvider = resourceProvider,
                cameraPermissionChecker = cameraPermissionChecker,
                writeStoragePermissionChecker = writeStoragePermissionChecker,
                imagePicker = imagePicker,
                flowRouter = flowRouter,
                context = context,
            )
        )
            .eventSources(
                cameraPermissionResultEventSource,
                writeStoragePermissionResultEventSource,
                imagePickerResultEventSource,
                photoPreviewDependencies.resultEmitter,
                howToGetPhotoResultEventSource,
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
        render = this
    )

    val diffuserFragmentDelegate = DiffuserFragmentDelegate(this)

    eventSender = controllerFragmentDelegate
    diffuserProvider = { diffuserFragmentDelegate.diffuser }
    delegates = setOf(
        diffuserFragmentDelegate,
        controllerFragmentDelegate,
        cameraPermissionChecker,
        writeStoragePermissionChecker,
        imagePicker,
        photoPreviewDependencies,
    )
}

class PhotoPreviewForPatientDetailsDependencies(
    override val resultEmitter: ResultEventSource<PhotoPreviewResult, PatientDetailsEvent>
) : SimpleFragmentDelegate(),
    PhotoPreviewDependencies