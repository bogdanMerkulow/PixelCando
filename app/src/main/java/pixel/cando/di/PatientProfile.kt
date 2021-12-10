package pixel.cando.di

import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.R
import pixel.cando.data.local.SessionWiper
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui._base.tea.ResultEventSource
import pixel.cando.ui._base.tea.emitUnit
import pixel.cando.ui.main.patient_profile.PatientProfileDataModel
import pixel.cando.ui.main.patient_profile.PatientProfileEffect
import pixel.cando.ui.main.patient_profile.PatientProfileEvent
import pixel.cando.ui.main.patient_profile.PatientProfileFragment
import pixel.cando.ui.main.patient_profile.PatientProfileLogic
import pixel.cando.ui.main.patient_profile.PatientProfileViewModel
import pixel.cando.ui.main.patient_profile.viewModel
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer


fun PatientProfileFragment.setup(
    sessionWiper: SessionWiper,
    rootRouter: RootRouter,
    resourceProvider: ResourceProvider,
    remoteRepository: RemoteRepository,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val logoutConfirmationResultEventSource = ResultEventSource<Unit, PatientProfileEvent> {
        PatientProfileEvent.LogoutConfirmed
    }

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PatientProfileViewModel,
            PatientProfileDataModel,
            PatientProfileEvent,
            PatientProfileEffect>(
        loop = Mobius.loop(
            Update<PatientProfileDataModel, PatientProfileEvent, PatientProfileEffect> { model, event ->
                PatientProfileLogic.update(
                    model,
                    event
                )
            },
            PatientProfileLogic.effectHandler(
                sessionWiper = sessionWiper,
                rootRouter = rootRouter,
                remoteRepository = remoteRepository,
                messageDisplayer = messageDisplayer,
                resourceProvider = resourceProvider,
                logoutConfirmationAsker = {
                    lifecycleScope.launch {
                        lifecycleScope.launch {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.logout_dialog_title)
                                .setMessage(R.string.logout_dialog_message)
                                .setPositiveButton(
                                    R.string.logout_dialog_logout_btn_title
                                ) { _, _ ->
                                    logoutConfirmationResultEventSource.emitUnit()
                                }
                                .setNegativeButton(
                                    R.string.cancel
                                ) { _, _ ->
                                }
                                .create()
                                .show()
                        }
                    }
                },
            )
        )
            .eventSources(
                logoutConfirmationResultEventSource
            )
            .logger(AndroidLogger.tag("PatientProfile")),
        initialState = {
            PatientProfileLogic.init(it)
        },
        defaultStateProvider = {
            PatientProfileLogic.initialModel()
        },
        modelMapper = {
            it.viewModel(
                resourceProvider = resourceProvider,
            )
        },
        render = this
    )

    val diffuserFragmentDelegate = DiffuserFragmentDelegate(
        this
    )

    eventSender = controllerFragmentDelegate
    diffuserProvider = { diffuserFragmentDelegate.diffuser }
    delegates = setOf(
        diffuserFragmentDelegate,
        controllerFragmentDelegate,
    )

}