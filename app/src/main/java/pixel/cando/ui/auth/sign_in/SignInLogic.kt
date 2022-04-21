package pixel.cando.ui.auth.sign_in

import android.net.Uri
import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.local.AccessTokenStore
import pixel.cando.data.local.LoggedInUserIdStore
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.models.SignInFailure
import pixel.cando.data.remote.AuthRepository
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.Either
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.PermissionChecker
import pixel.cando.utils.PoseChecker
import pixel.cando.utils.PushNotificationsSubscriber
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onRight

object SignInLogic {

    fun init(
        model: SignInDataModel
    ): First<SignInDataModel, SignInEffect> {
        return First.first(model)
    }

    fun update(
        model: SignInDataModel,
        event: SignInEvent
    ): Next<SignInDataModel, SignInEffect> {
        return when (event) {
            // ui
            is SignInEvent.EmailChanged -> {
                if (event.email == model.email) Next.noChange()
                else Next.next(
                    model.copy(
                        email = event.email,
                        isSignInButtonEnabled = isSignInAllowed(
                            email = event.email,
                            password = model.password
                        )
                    )
                )
            }
            is SignInEvent.PasswordChanged -> {
                if (event.password == model.password) Next.noChange()
                else Next.next(
                    model.copy(
                        password = event.password,
                        isSignInButtonEnabled = isSignInAllowed(
                            email = model.email,
                            password = event.password
                        )
                    )
                )
            }
            is SignInEvent.TapSignIn -> {
                if (
                    isSignInAllowed(
                        email = model.email,
                        password = model.password
                    )
                ) {
                    Next.next(
                        model.copy(
                            isLoaderVisible = true,
                            isSignInButtonEnabled = false
                        ),
                        setOf(
                            SignInEffect.TrySignIn(
                                email = model.email,
                                password = model.password
                            )
                        )
                    )
                } else {
                    Next.noChange()
                }
            }
            is SignInEvent.TapRecoverPassword -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.NavigateToPasswordRecovery(
                            email = model.email
                        )
                    )
                )
            }
            is SignInEvent.TapTakePhoto -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.CheckCameraPermission
                    )
                )
            }
            is SignInEvent.PhotoTaken -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.CheckPoseInPhoto(
                            uri = event.uri
                        )
                    )
                )
            }
            is SignInEvent.PoseInPhotoChecked -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.ShowTakePhotoSuccessMessage
                    )
                )
            }
            // model
            is SignInEvent.SignInSucceeded -> {
                Next.next(
                    model.copy(
                        isLoaderVisible = false,
                    ),
                    setOf(
                        SignInEffect.NavigateToHome
                    )
                )
            }
            is SignInEvent.SignInFailed -> {
                Next.next(
                    model.copy(
                        isLoaderVisible = false,
                        isSignInButtonEnabled = isSignInAllowed(
                            email = model.email,
                            password = model.password
                        )
                    )
                )
            }
            is SignInEvent.CameraPermissionGranted -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.CheckWriteStoragePermission
                    )
                )
            }
            is SignInEvent.CameraPermissionDenied -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.ShowUnexpectedError // TODO change the message
                    )
                )
            }
            is SignInEvent.WriteStoragePermissionGranted -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.OpenPhotoTaker
                    )
                )
            }
            is SignInEvent.WriteStoragePermissionDenied -> {
                Next.dispatch(
                    setOf(
                        SignInEffect.ShowUnexpectedError // TODO change the message
                    )
                )
            }
        }
    }

    fun effectHandler(
        rootRouter: RootRouter,
        flowRouter: FlowRouter,
        authRepository: AuthRepository,
        accessTokenStore: AccessTokenStore,
        userRoleStore: UserRoleStore,
        loggedInUserIdStore: LoggedInUserIdStore,
        messageDisplayer: MessageDisplayer,
        pushNotificationsSubscriber: PushNotificationsSubscriber,
        resourceProvider: ResourceProvider,
        photoTakerOpener: () -> Unit,
        poseAnalyserOpener: (Uri) -> Unit,
        poseChecker: PoseChecker,
        takePhotoSuccessMessageDisplayer: () -> Unit,
        cameraPermissionChecker: PermissionChecker,
        writeStoragePermissionChecker: PermissionChecker,
    ): Connectable<SignInEffect, SignInEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is SignInEffect.TrySignIn -> {
                    val result = authRepository.signIn(
                        email = effect.email,
                        password = effect.password
                    )
                    when (result) {
                        is Either.Left -> {
                            accessTokenStore.accessToken = result.left.accessToken
                            userRoleStore.userRole = result.left.userRole
                            loggedInUserIdStore.loggedInUserId = result.left.userId

                            val pushNotificationSubscriptionResult =
                                pushNotificationsSubscriber.subscribe()
                            pushNotificationSubscriptionResult.onRight {
                                logError(it)
                            }

                            output.accept(
                                SignInEvent.SignInSucceeded
                            )
                        }
                        is Either.Right -> {
                            when (val signInFailure = result.right) {
                                is SignInFailure.CustomMessage -> {
                                    messageDisplayer.showMessage(signInFailure.message)
                                }
                                is SignInFailure.UnsupportedUserRole -> {
                                    messageDisplayer.showMessage("Unsupported role")
                                }
                                is SignInFailure.UnknownError -> {
                                    logError(signInFailure.throwable)
                                }
                            }
                            output.accept(
                                SignInEvent.SignInFailed
                            )
                        }
                    }

                }
                is SignInEffect.NavigateToHome -> {
                    rootRouter.replaceScreen(
                        Screens.mainFlow()
                    )
                }
                is SignInEffect.NavigateToPasswordRecovery -> {
                    flowRouter.navigateTo(
                        Screens.passwordRecovery(
                            email = effect.email
                        )
                    )
                }
                is SignInEffect.OpenPhotoTaker -> {
                    photoTakerOpener.invoke()
                }
                is SignInEffect.CheckPoseInPhoto -> {
                    val result = poseChecker.check(effect.uri)
                    if (result.success) {
                        output.accept(
                            SignInEvent.PoseInPhotoChecked(
                                uri = effect.uri
                            )
                        )
                    } else {
                        poseAnalyserOpener.invoke(effect.uri)
                    }
                }
                is SignInEffect.CheckCameraPermission -> {
                    if (cameraPermissionChecker.checkPermission()) {
                        output.accept(
                            SignInEvent.CameraPermissionGranted
                        )
                    } else {
                        cameraPermissionChecker.requestPermission()
                    }
                }
                is SignInEffect.CheckWriteStoragePermission -> {
                    if (writeStoragePermissionChecker.checkPermission()) {
                        output.accept(
                            SignInEvent.WriteStoragePermissionGranted
                        )
                    } else {
                        writeStoragePermissionChecker.requestPermission()
                    }
                }
                is SignInEffect.ShowTakePhotoSuccessMessage -> {
                    takePhotoSuccessMessageDisplayer.invoke()
                }
                is SignInEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
            }
        }

    fun initialModel(
    ) = SignInDataModel(
        email = "",
        password = "",
        isLoaderVisible = false,
        isSignInButtonEnabled = false
    )

}

sealed class SignInEvent {
    // ui
    data class EmailChanged(
        val email: String
    ) : SignInEvent()

    data class PasswordChanged(
        val password: String
    ) : SignInEvent()

    object TapSignIn : SignInEvent()

    object TapTakePhoto : SignInEvent()

    object TapRecoverPassword : SignInEvent()

    data class PhotoTaken(
        val uri: Uri
    ) : SignInEvent()

    data class PoseInPhotoChecked(
        val uri: Uri
    ) : SignInEvent()

    // model
    object SignInSucceeded : SignInEvent()
    object SignInFailed : SignInEvent()

    object CameraPermissionGranted : SignInEvent()
    object CameraPermissionDenied : SignInEvent()

    object WriteStoragePermissionGranted : SignInEvent()
    object WriteStoragePermissionDenied : SignInEvent()
}

sealed class SignInEffect {
    data class TrySignIn(
        val email: String,
        val password: String
    ) : SignInEffect()

    object NavigateToHome : SignInEffect()

    data class NavigateToPasswordRecovery(
        val email: String,
    ) : SignInEffect()

    object CheckCameraPermission : SignInEffect()

    object CheckWriteStoragePermission : SignInEffect()

    object OpenPhotoTaker : SignInEffect()

    data class CheckPoseInPhoto(
        val uri: Uri,
    ) : SignInEffect()

    object ShowTakePhotoSuccessMessage: SignInEffect()

    object ShowUnexpectedError : SignInEffect()

}

@Parcelize
data class SignInDataModel(
    val email: String,
    val password: String,
    val isLoaderVisible: Boolean,
    val isSignInButtonEnabled: Boolean
) : Parcelable

data class SignInViewModel(
    val email: String,
    val password: String,
    val isLoaderVisible: Boolean,
    val isSignInButtonEnabled: Boolean
)

val SignInDataModel.viewModel: SignInViewModel
    get() = SignInViewModel(
        email = email,
        password = password,
        isLoaderVisible = isLoaderVisible,
        isSignInButtonEnabled = isSignInButtonEnabled
    )

private fun isSignInAllowed(
    email: String,
    password: String
) = email.isNotBlank()
        && password.isNotBlank()