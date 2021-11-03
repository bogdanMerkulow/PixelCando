package pixel.cando.di

import android.Manifest
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.ui.main.photo_list.PhotoListDataModel
import pixel.cando.ui.main.photo_list.PhotoListEffect
import pixel.cando.ui.main.photo_list.PhotoListEvent
import pixel.cando.ui.main.photo_list.PhotoListFragment
import pixel.cando.ui.main.photo_list.PhotoListLogic
import pixel.cando.ui.main.photo_list.PhotoListViewModel
import pixel.cando.ui.main.photo_list.viewModel
import pixel.cando.utils.PermissionCheckerResult
import pixel.cando.utils.RealPermissionChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.createPermissionCheckerResultEventSource
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun setup(
    fragment: PhotoListFragment,
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    context: Context,
) {
    if (fragment.delegates.isNotEmpty()) {
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
                    fragment.lifecycleScope.launch {
                        CameraFragment.show(
                            fragment.childFragmentManager
                        )
                    }
                },
                remoteRepository = remoteRepository,
                messageDisplayer = fragment.messageDisplayer,
                resourceProvider = resourceProvider,
                cameraPermissionChecker = cameraPermissionChecker,
            )
        )
            .eventSources(
                cameraPermissionResultEventSource
            )
            .logger(AndroidLogger.tag("PhotoList")),
        initialState = {
            PhotoListLogic.init(it)
        },
        defaultStateProvider = {
            PhotoListLogic.initialModel()
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
        cameraPermissionChecker,
    )
}