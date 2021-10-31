package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.remote.AuthRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.auth.password_recovery.*
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun setup(
    fragment: PasswordRecoveryFragment,
    authRepository: AuthRepository,
    resourceProvider: ResourceProvider,
    flowRouter: FlowRouter,
) {
    if (fragment.delegates.isNotEmpty()) {
        return
    }
    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PasswordRecoveryViewModel,
            PasswordRecoveryDataModel,
            PasswordRecoveryEvent,
            PasswordRecoveryEffect>(
        loop = Mobius.loop(
            Update<PasswordRecoveryDataModel, PasswordRecoveryEvent, PasswordRecoveryEffect> { model, event ->
                PasswordRecoveryLogic.update(
                    model,
                    event
                )
            },
            PasswordRecoveryLogic.effectHandler(
                authRepository = authRepository,
                resourceProvider = resourceProvider,
                messageDisplayer = fragment.messageDisplayer,
                flowRouter = flowRouter,
            )
        )
            .logger(AndroidLogger.tag("PasswordRecovery")),
        initialState = {
            PasswordRecoveryLogic.init(it)
        },
        defaultStateProvider = {
            PasswordRecoveryLogic.initialModel(
                email = fragment.getArgument()
            )
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