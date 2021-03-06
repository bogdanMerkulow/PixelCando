package pixel.cando.ui.auth.password_recovery

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.PasswordRecoveryFailure
import pixel.cando.data.remote.AuthRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight

object PasswordRecoveryLogic {

    fun init(
        model: PasswordRecoveryDataModel
    ): First<PasswordRecoveryDataModel, PasswordRecoveryEffect> {
        return First.first(model)
    }

    fun update(
        model: PasswordRecoveryDataModel,
        event: PasswordRecoveryEvent
    ): Next<PasswordRecoveryDataModel, PasswordRecoveryEffect> {
        return when (event) {
            // ui
            is PasswordRecoveryEvent.EmailChanged -> {
                if (event.email == model.email) Next.noChange()
                else Next.next(
                    model.copy(
                        email = event.email
                    )
                )
            }
            is PasswordRecoveryEvent.TapRecover -> {
                Next.next(
                    model.copy(
                        isLoading = true
                    ),
                    setOf(
                        PasswordRecoveryEffect.SendPasswordRecoveryEmail(
                            email = model.email
                        )
                    )
                )
            }
            is PasswordRecoveryEvent.TapExit -> {
                Next.dispatch(
                    setOf(
                        PasswordRecoveryEffect.Exit
                    )
                )
            }
            // model
            is PasswordRecoveryEvent.SendPasswordRecoveryEmailSuccess -> {
                Next.next(
                    model.copy(
                        isLoading = false
                    ),
                    setOf(
                        PasswordRecoveryEffect.ShowSuccessMessage,
                        PasswordRecoveryEffect.Exit
                    )
                )
            }
            is PasswordRecoveryEvent.SendPasswordRecoveryEmailFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false
                    ),
                    setOf(
                        event.message?.let {
                            PasswordRecoveryEffect.ShowMessage(it)
                        } ?: PasswordRecoveryEffect.ShowUnexpectedError
                    )
                )
            }
        }
    }

    fun effectHandler(
        authRepository: AuthRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        flowRouter: FlowRouter,
    ): Connectable<PasswordRecoveryEffect, PasswordRecoveryEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PasswordRecoveryEffect.SendPasswordRecoveryEmail -> {
                    val result = authRepository.recoverPassword(
                        email = effect.email
                    )
                    result.onLeft {
                        output.accept(
                            PasswordRecoveryEvent.SendPasswordRecoveryEmailSuccess
                        )
                    }
                    result.onRight {
                        when (it) {
                            is PasswordRecoveryFailure.CustomMessage -> {
                                output.accept(
                                    PasswordRecoveryEvent.SendPasswordRecoveryEmailFailure(
                                        it.message
                                    )
                                )
                            }
                            is PasswordRecoveryFailure.UnknownError -> {
                                logError(it.throwable)
                                output.accept(
                                    PasswordRecoveryEvent.SendPasswordRecoveryEmailFailure(null)
                                )
                            }
                        }
                    }
                }
                is PasswordRecoveryEffect.ShowMessage -> {
                    messageDisplayer.showMessage(
                        effect.message
                    )
                }
                is PasswordRecoveryEffect.ShowSuccessMessage -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.password_recovery_success_message)
                    )
                }
                is PasswordRecoveryEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
                is PasswordRecoveryEffect.Exit -> {
                    flowRouter.exit()
                }
            }
        }

    fun initialModel(
        email: String
    ) = PasswordRecoveryDataModel(
        email = email,
        isLoading = false,
    )

}

sealed class PasswordRecoveryEvent {
    // ui
    data class EmailChanged(
        val email: String
    ) : PasswordRecoveryEvent()

    object TapRecover : PasswordRecoveryEvent()
    object TapExit : PasswordRecoveryEvent()

    // model
    object SendPasswordRecoveryEmailSuccess : PasswordRecoveryEvent()
    data class SendPasswordRecoveryEmailFailure(
        val message: String?
    ) : PasswordRecoveryEvent()
}

sealed class PasswordRecoveryEffect {
    data class SendPasswordRecoveryEmail(
        val email: String
    ) : PasswordRecoveryEffect()

    object ShowUnexpectedError : PasswordRecoveryEffect()

    object ShowSuccessMessage : PasswordRecoveryEffect()

    data class ShowMessage(
        val message: String
    ) : PasswordRecoveryEffect()

    object Exit : PasswordRecoveryEffect()
}

@Parcelize
data class PasswordRecoveryDataModel(
    val email: String,
    val isLoading: Boolean,
) : Parcelable

data class PasswordRecoveryViewModel(
    val email: String,
    val isLoaderVisible: Boolean,
    val isRecoveryButtonEnabled: Boolean,
)

val PasswordRecoveryDataModel.viewModel: PasswordRecoveryViewModel
    get() = PasswordRecoveryViewModel(
        email = email,
        isLoaderVisible = isLoading,
        isRecoveryButtonEnabled = isLoading.not() && email.isNotBlank()
    )