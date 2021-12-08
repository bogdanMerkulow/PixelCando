package pixel.cando.ui.main.chat_with_patient

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight

object ChatWithPatientLogic {

    fun init(
        model: ChatWithPatientDataModel
    ): First<ChatWithPatientDataModel, ChatWithPatientEffect> {
        return if (model.patientFullName == null) {
            First.first(
                model,
                setOf(
                    ChatWithPatientEffect.LoadPatientFullName(model.patientId)
                )
            )
        } else {
            First.first(model)
        }
    }

    fun update(
        model: ChatWithPatientDataModel,
        event: ChatWithPatientEvent
    ): Next<ChatWithPatientDataModel, ChatWithPatientEffect> {
        return when (event) {
            // ui
            is ChatWithPatientEvent.TapExit -> {
                Next.dispatch(
                    setOf(
                        ChatWithPatientEffect.Exit
                    )
                )
            }
            // model
            is ChatWithPatientEvent.PatientNameReceived -> {
                Next.next(
                    model.copy(
                        patientFullName = event.fullName
                    )
                )
            }
        }
    }

    fun effectHandler(
        remoteRepository: RemoteRepository,
        flowRouter: FlowRouter,
    ): Connectable<ChatWithPatientEffect, ChatWithPatientEvent> {
        return CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is ChatWithPatientEffect.LoadPatientFullName -> {
                    val result = remoteRepository.getPatient(effect.patientId)
                    result.onLeft {
                        output.accept(
                            ChatWithPatientEvent.PatientNameReceived(
                                it.fullName
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                    }
                }
                is ChatWithPatientEffect.Exit -> {
                    flowRouter.exit()
                }
            }
        }
    }

    fun initialModel(
        patientId: Long
    ) = ChatWithPatientDataModel(
        patientId = patientId,
        patientFullName = null,
    )

}

sealed class ChatWithPatientEvent {
    // ui
    object TapExit : ChatWithPatientEvent()

    // model
    data class PatientNameReceived(
        val fullName: String,
    ) : ChatWithPatientEvent()
}

sealed class ChatWithPatientEffect {

    data class LoadPatientFullName(
        val patientId: Long
    ) : ChatWithPatientEffect()

    object Exit : ChatWithPatientEffect()

}

@Parcelize
data class ChatWithPatientDataModel(
    val patientId: Long,
    val patientFullName: String?
) : Parcelable

data class ChatWithPatientViewModel(
    val title: String?
)

fun ChatWithPatientDataModel.viewModel(
) = ChatWithPatientViewModel(
    title = patientFullName,
)