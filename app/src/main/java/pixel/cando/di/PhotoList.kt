package pixel.cando.di

import android.Manifest
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.R
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.SimpleFragmentDelegate
import pixel.cando.ui._base.fragment.withArgumentSet
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui._base.tea.ResultEventSource
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.ui.main.photo_list.PhotoListDataModel
import pixel.cando.ui.main.photo_list.PhotoListEffect
import pixel.cando.ui.main.photo_list.PhotoListEvent
import pixel.cando.ui.main.photo_list.PhotoListFragment
import pixel.cando.ui.main.photo_list.PhotoListLogic
import pixel.cando.ui.main.photo_list.PhotoListViewModel
import pixel.cando.ui.main.photo_list.viewModel
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

fun PhotoListFragment.setup(
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    context: Context,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val cameraPermissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> PhotoListEvent.CameraPermissionGranted
            is PermissionCheckerResult.Denied -> PhotoListEvent.CameraPermissionDenied
        }
    }
    val cameraPermissionChecker = RealPermissionChecker(
        permission = Manifest.permission.CAMERA,
        context = context,
        resultEmitter = cameraPermissionResultEventSource
    )

    val writeStoragePermissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> PhotoListEvent.WriteStoragePermissionGranted
            is PermissionCheckerResult.Denied -> PhotoListEvent.WriteStoragePermissionDenied
        }
    }
    val writeStoragePermissionChecker = RealPermissionChecker(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        context = context,
        resultEmitter = writeStoragePermissionResultEventSource
    )

    val imagePickerResultEventSource = createImagePickerResultEventSource<PhotoListEvent> {
        PhotoListEvent.ImagePicked(it.uri)
    }

    val imagePicker = RealImagePicker(
        resultEmitter = imagePickerResultEventSource,
    )

    val photoPreviewDependencies = PhotoPreviewForPhotoListDependencies(
        resultEmitter = ResultEventSource {
            PhotoListEvent.PhotoAccepted(
                uri = it.uri,
                weight = it.weight,
            )
        }
    )

    val howToGetPhotoResultEventSource = ResultEventSource<Int, PhotoListEvent> {
        when (it) {
            0 -> PhotoListEvent.PhotoTakingChosen
            1 -> PhotoListEvent.ImagePickingChosen
            else -> error("Illegal index received")
        }
    }

    val photoRemovalConfirmationResultEventSource = ResultEventSource<Long, PhotoListEvent> {
        PhotoListEvent.PhotoDeletionConfirmed(it)
    }

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PhotoListViewModel,
            PhotoListDataModel,
            PhotoListEvent,
            PhotoListEffect>(
        loop = Mobius.loop(
            Update<PhotoListDataModel, PhotoListEvent, PhotoListEffect> { model, event ->
                PhotoListLogic.update(
                    model,
                    event
                )
            },
            PhotoListLogic.effectHandler(
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
                        MaterialAlertDialogBuilder(requireContext())
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
                photoRemovalConfirmationAsker = { photoId ->
                    lifecycleScope.launch {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.delete_photo_dialog_title)
                            .setMessage(R.string.delete_photo_dialog_message)
                            .setPositiveButton(
                                R.string.delete_photo_dialog_confirm_btn_title
                            ) { _, _ ->
                                photoRemovalConfirmationResultEventSource.emit(photoId)
                            }
                            .setNegativeButton(
                                R.string.cancel
                            ) { _, _ ->
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
                context = context,
            )
        )
            .eventSources(
                cameraPermissionResultEventSource,
                writeStoragePermissionResultEventSource,
                imagePickerResultEventSource,
                photoPreviewDependencies.resultEmitter,
                howToGetPhotoResultEventSource,
                photoRemovalConfirmationResultEventSource,
            )
            .logger(AndroidLogger.tag("PhotoList")),
        initialState = {
            PhotoListLogic.init(it)
        },
        defaultStateProvider = {
            PhotoListLogic.initialModel()
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


class PhotoPreviewForPhotoListDependencies(
    override val resultEmitter: ResultEventSource<PhotoPreviewResult, PhotoListEvent>
) : SimpleFragmentDelegate(),
    PhotoPreviewDependencies