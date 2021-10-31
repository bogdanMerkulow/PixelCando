package pixel.cando.ui.main.profile

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.data.local.SessionWiper
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler

object ProfileLogic {

    fun init(
        model: ProfileDataModel
    ): First<ProfileDataModel, ProfileEffect> {
        return First.first(model)
    }

    fun update(
        model: ProfileDataModel,
        event: ProfileEvent
    ): Next<ProfileDataModel, ProfileEffect> {
        return when (event) {
            is ProfileEvent.LogoutTap -> {
                Next.dispatch(
                    setOf(
                        ProfileEffect.Logout
                    )
                )
            }
        }
    }

    fun effectHandler(
        sessionWiper: SessionWiper,
        rootRouter: RootRouter,
    ): Connectable<ProfileEffect, ProfileEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is ProfileEffect.Logout -> {
                    sessionWiper.wipe()
                    rootRouter.replaceScreen(
                        Screens.authFlow()
                    )
                }
            }
        }

    fun initialModel(
    ) = ProfileDataModel()

}

sealed class ProfileEvent {
    object LogoutTap : ProfileEvent()
}

sealed class ProfileEffect {
    object Logout : ProfileEffect()
}

@Parcelize
class ProfileDataModel : Parcelable

class ProfileViewModel

fun ProfileDataModel.viewModel() = ProfileViewModel()