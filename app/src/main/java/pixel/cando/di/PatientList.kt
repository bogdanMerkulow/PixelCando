package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.patient_list.*
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun setup(
    fragment: PatientListFragment,
    remoteRepository: RemoteRepository,
) {
    if (fragment.delegates.isNotEmpty()) {
        return
    }
    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PatientListViewModel,
            PatientListDataModel,
            PatientListEvent,
            PatientListEffect>(
        loop = Mobius.loop(
            Update<PatientListDataModel, PatientListEvent, PatientListEffect> { model, event ->
                PatientListLogic.update(
                    model,
                    event
                )
            },
            PatientListLogic.effectHandler(
                remoteRepository = remoteRepository,
                messageDisplayer = fragment.messageDisplayer,
            )
        )
            .logger(AndroidLogger.tag("PatientList")),
        initialState = {
            PatientListLogic.init(it)
        },
        defaultStateProvider = {
            PatientListLogic.initialModel()
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