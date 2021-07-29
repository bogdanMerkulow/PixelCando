package pixel.cando.ui.auth.sign_in

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.data.local.AccessTokenStore
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui._models.SignInFailure
import pixel.cando.utils.Either
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.logError

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
        }
    }

    fun effectHandler(
        rootRouter: RootRouter,
        remoteRepository: RemoteRepository,
        accessTokenStore: AccessTokenStore,
        userRoleStore: UserRoleStore,
        messageDisplayer: MessageDisplayer,
    ): Connectable<SignInEffect, SignInEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is SignInEffect.TrySignIn -> {
                    val result = remoteRepository.signIn(
                        email = effect.email,
                        password = effect.password
                    )
                    when (result) {
                        is Either.Left -> {
                            accessTokenStore.accessToken = result.left.accessToken
                            userRoleStore.userRole = result.left.userRole

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

    // model
    object SignInSucceeded : SignInEvent()
    object SignInFailed : SignInEvent()
}

sealed class SignInEffect {
    data class TrySignIn(
        val email: String,
        val password: String
    ) : SignInEffect()

    object NavigateToHome : SignInEffect()
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