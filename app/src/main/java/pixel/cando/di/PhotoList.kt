package pixel.cando.di

import android.content.Context
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.ui.main.photo_list.*
import pixel.cando.utils.*
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate

fun setup(
    fragment: PhotoListFragment,
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    context: Context,
) {
    if (fragment.delegates.isNotEmpty()) {
        return
    }
    val permissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> PhotoListEvent.CameraPermissionGranted
            is PermissionCheckerResult.Denied -> PhotoListEvent.CameraPermissionDenied
        }
    }
    val permissionChecker = RealPermissionChecker(
        context = context,
        resultEmitter = permissionResultEventSource
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
            )
        )
            .eventSources(
                permissionResultEventSource
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
        permissionChecker,
    )
}