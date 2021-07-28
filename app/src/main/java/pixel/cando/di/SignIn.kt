package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.local.AccessTokenStore
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.auth.sign_in.*
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun setup(
    fragment: SignInFragment,
    rootRouter: RootRouter,
    remoteRepository: RemoteRepository,
    accessTokenStore: AccessTokenStore,
    userRoleStore: UserRoleStore,
) {
    if (fragment.delegates.isNotEmpty()) {
        return
    }
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
                remoteRepository = remoteRepository,
                accessTokenStore = accessTokenStore,
                userRoleStore = userRoleStore,
                messageDisplayer = fragment.messageDisplayer,
            )
        )
            .logger(AndroidLogger.tag("SignIn")),
        initialState = {
            SignInLogic.init(it)
        },
        defaultStateProvider = {
            SignInDataModel.initial
        },
        modelMapper = {
            it.viewModel
        },
        render = fragment
    )

    val viewBindingFragmentDelegate = DiffuserFragmentDelegate(
        fragment
    )

    fragment.eventSender = controllerFragmentDelegate
    fragment.diffuserProvider = { viewBindingFragmentDelegate.diffuser }
    fragment.delegates = setOf(
        controllerFragmentDelegate,
        viewBindingFragmentDelegate
    )
}