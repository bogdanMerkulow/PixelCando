package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.fragment.withArgumentSet
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.chat_messaging.ChatMessagingFragment
import pixel.cando.ui.main.chat_with_patient.ChatWithPatientDataModel
import pixel.cando.ui.main.chat_with_patient.ChatWithPatientEffect
import pixel.cando.ui.main.chat_with_patient.ChatWithPatientEvent
import pixel.cando.ui.main.chat_with_patient.ChatWithPatientFragment
import pixel.cando.ui.main.chat_with_patient.ChatWithPatientLogic
import pixel.cando.ui.main.chat_with_patient.ChatWithPatientViewModel
import pixel.cando.ui.main.chat_with_patient.viewModel

fun ChatWithPatientFragment.setup(
    remoteRepository: RemoteRepository,
    flowRouter: FlowRouter,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val patientId = getArgument<Long>()

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            ChatWithPatientViewModel,
            ChatWithPatientDataModel,
            ChatWithPatientEvent,
            ChatWithPatientEffect>(
        loop = Mobius.loop(
            Update<ChatWithPatientDataModel, ChatWithPatientEvent, ChatWithPatientEffect> { model, event ->
                ChatWithPatientLogic.update(
                    model,
                    event
                )
            },
            ChatWithPatientLogic.effectHandler(
                remoteRepository = remoteRepository,
                flowRouter = flowRouter,
            )
        )
            .logger(AndroidLogger.tag("ChatWithPatient")),
        initialState = {
            ChatWithPatientLogic.init(it)
        },
        defaultStateProvider = {
            ChatWithPatientLogic.initialModel(
                patientId = patientId,
            )
        },
        modelMapper = {
            it.viewModel()
        },
        render = this
    )

    contentFragmentProvider = {
        ChatMessagingFragment()
            .withArgumentSet(patientId)
    }

    eventSender = controllerFragmentDelegate
    delegates = setOf(
        controllerFragmentDelegate,
    )
}