package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.local.AccessTokenStore
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.remote.AuthRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.auth.sign_in.*
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun setup(
    fragment: SignInFragment,
    rootRouter: RootRouter,
    flowRouter: FlowRouter,
    authRepository: AuthRepository,
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
                flowRouter = flowRouter,
                authRepository = authRepository,
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
            SignInLogic.initialModel()
        },
        modelMapper = {
            it.viewModel
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
    )
}