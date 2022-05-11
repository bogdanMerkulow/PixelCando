package pixel.cando.di

import android.Manifest
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.data.local.AccessTokenStore
import pixel.cando.data.local.LoggedInUserIdStore
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.remote.AuthRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.fragment.SimpleFragmentDelegate
import pixel.cando.ui._base.fragment.withArgumentSet
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui._base.tea.ResultEventSource
import pixel.cando.ui.auth.sign_in.SignInDataModel
import pixel.cando.ui.auth.sign_in.SignInEffect
import pixel.cando.ui.auth.sign_in.SignInEvent
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.auth.sign_in.SignInLogic
import pixel.cando.ui.auth.sign_in.SignInViewModel
import pixel.cando.ui.auth.sign_in.viewModel
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.ui.main.pose_analysis.PoseAnalysisFragment
import pixel.cando.ui.main.pose_analysis.PoseAnalysisResult
import pixel.cando.utils.PermissionCheckerResult
import pixel.cando.utils.PoseChecker
import pixel.cando.utils.PushNotificationsSubscriber
import pixel.cando.utils.RealPermissionChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.createPermissionCheckerResultEventSource
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun SignInFragment.setup(
    rootRouter: RootRouter,
    flowRouter: FlowRouter,
    authRepository: AuthRepository,
    accessTokenStore: AccessTokenStore,
    userRoleStore: UserRoleStore,
    loggedInUserIdStore: LoggedInUserIdStore,
    poseChecker: PoseChecker,
    pushNotificationsSubscriber: PushNotificationsSubscriber,
    resourceProvider: ResourceProvider,
    context: Context,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val cameraPermissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> SignInEvent.CameraPermissionGranted
            is PermissionCheckerResult.Denied -> SignInEvent.CameraPermissionDenied
        }
    }
    val cameraPermissionChecker = RealPermissionChecker(
        permission = Manifest.permission.CAMERA,
        context = context,
        resultEmitter = cameraPermissionResultEventSource
    )

    val writeStoragePermissionResultEventSource = createPermissionCheckerResultEventSource {
        when (it) {
            is PermissionCheckerResult.Granted -> SignInEvent.WriteStoragePermissionGranted
            is PermissionCheckerResult.Denied -> SignInEvent.WriteStoragePermissionDenied
        }
    }
    val writeStoragePermissionChecker = RealPermissionChecker(
        permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
        context = context,
        resultEmitter = writeStoragePermissionResultEventSource
    )

    val poseAnalysisDependencies = PoseAnalysisForSignInDependencies(
        resultEmitter = ResultEventSource {
            SignInEvent.PoseInPhotoChecked(
                uri = it.uri,
            )
        }
    )

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            SignInViewModel,
            SignInDataModel,
            SignInEvent,
            SignInEffect>(
        loop = Mobius.loop(
            Update<SignInDataModel, SignInEvent, SignInEffect> { model, event ->
                SignInLogic.update(
                    model,
                    event
                )
            },
            SignInLogic.effectHandler(
                rootRouter = rootRouter,
                flowRouter = flowRouter,
                authRepository = authRepository,
                accessTokenStore = accessTokenStore,
                userRoleStore = userRoleStore,
                loggedInUserIdStore = loggedInUserIdStore,
                messageDisplayer = messageDisplayer,
                pushNotificationsSubscriber = pushNotificationsSubscriber,
                resourceProvider = resourceProvider,
                photoTakerOpener = {
                    lifecycleScope.launch {
                        CameraFragment.show(
                            childFragmentManager
                        )
                    }
                },
                poseAnalyserOpener = {
                    lifecycleScope.launch {
                        PoseAnalysisFragment()
                            .withArgumentSet(it)
                            .show(childFragmentManager, "")
                    }
                },
                poseChecker = poseChecker,
                takePhotoSuccessMessageDisplayer = {
                    lifecycleScope.launch {
                        showTakePhotoSuccessMessage()
                    }
                },
                cameraPermissionChecker = cameraPermissionChecker,
                writeStoragePermissionChecker = writeStoragePermissionChecker,
            )
        )
            .eventSources(
                cameraPermissionResultEventSource,
                writeStoragePermissionResultEventSource,
                poseAnalysisDependencies.resultEmitter,
            )
            .logger(AndroidLogger.tag("SignIn")),
        initialState = {
            SignInLogic.init(it)
        },
        defaultStateProvider = {
            SignInLogic.initialModel()
        },
        modelMapper = {
            it.viewModel
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
        poseAnalysisDependencies,
    )
}

private class PoseAnalysisForSignInDependencies(
    override val resultEmitter: ResultEventSource<PoseAnalysisResult, SignInEvent>
) : SimpleFragmentDelegate(),
    PoseAnalysisDependencies