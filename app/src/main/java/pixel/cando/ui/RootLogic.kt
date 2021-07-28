package pixel.cando.ui

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.data.local.AuthStateChecker
import pixel.cando.data.local.SessionWiper
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler

object RootLogic {

    fun init(
        model: RootDataModel
    ): First<RootDataModel, RootEffect> {
        return if (model.flow == null) {
            First.first(
                model,
                setOf(RootEffect.CheckAuthState)
            )
        } else {
            First.first(
                model
            )
        }
    }

    fun update(
        model: RootDataModel,
        event: RootEvent
    ): Next<RootDataModel, RootEffect> {
        return when (event) {
            is RootEvent.UserIsAuthorized -> {
                Next.next(
                    model.copy(
                        flow = RootFlow.HOME
                    ),
                    setOf(
                        RootEffect.NavigateToHomeFlow
                    )
                )
            }
            is RootEvent.UserIsNotAuthorized -> {
                Next.next(
                    model.copy(
                        flow = RootFlow.AUTH
                    ),
                    setOf(
                        RootEffect.NavigateToAuthFlow
                    )
                )
            }
            is RootEvent.UserAuthorizationGotInvalid -> {
                Next.next(
                    model.copy(
                        flow = RootFlow.AUTH
                    ),
                    setOf(
                        RootEffect.ClearSession,
                        RootEffect.NavigateToAuthFlow
                    )
                )
            }
        }
    }

    fun effectHandler(
        rootRouter: RootRouter,
        authStateChecker: AuthStateChecker,
        sessionWiper: SessionWiper,
    ): Connectable<RootEffect, RootEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is RootEffect.CheckAuthState -> {
                    output.accept(
                        if (authStateChecker.isAuthorized) RootEvent.UserIsAuthorized
                        else RootEvent.UserIsNotAuthorized
                    )
                }
                is RootEffect.NavigateToAuthFlow -> {
                    rootRouter.replaceScreen(
                        Screens.authFlow()
                    )
                }
                is RootEffect.NavigateToHomeFlow -> {
                    rootRouter.replaceScreen(
                        Screens.homeFlow()
                    )
                }
                is RootEffect.ClearSession -> {
                    sessionWiper.wipe()
                }
            }
        }

}

sealed class RootEvent {
    // model
    object UserIsAuthorized : RootEvent()
    object UserIsNotAuthorized : RootEvent()
    object UserAuthorizationGotInvalid : RootEvent()
}

sealed class RootEffect {
    object CheckAuthState : RootEffect()
    object NavigateToAuthFlow : RootEffect()
    object NavigateToHomeFlow : RootEffect()
    object ClearSession : RootEffect()
}

@Parcelize
data class RootDataModel(
    val flow: RootFlow?
) : Parcelable {

    companion object {
        val initial: RootDataModel
            get() = RootDataModel(
                flow = null
            )
    }

}

object RootViewModel

val RootDataModel.viewModel: RootViewModel
    get() = RootViewModel

enum class RootFlow {
    AUTH, HOME
}
