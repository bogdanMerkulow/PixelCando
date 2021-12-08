package pixel.cando.di

import androidx.lifecycle.lifecycleScope
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.data.local.LoggedInUserIdProvider
import pixel.cando.data.models.MessageListPortion
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.getOptionalArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.chat_messaging.ChatMessagingDataModel
import pixel.cando.ui.main.chat_messaging.ChatMessagingDataSource
import pixel.cando.ui.main.chat_messaging.ChatMessagingEffect
import pixel.cando.ui.main.chat_messaging.ChatMessagingEvent
import pixel.cando.ui.main.chat_messaging.ChatMessagingFragment
import pixel.cando.ui.main.chat_messaging.ChatMessagingLogic
import pixel.cando.ui.main.chat_messaging.ChatMessagingViewModel
import pixel.cando.ui.main.chat_messaging.viewModel
import pixel.cando.utils.Either
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer
import java.time.LocalDateTime

fun ChatMessagingFragment.setup(
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    loggedInUserIdProvider: LoggedInUserIdProvider,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val dataSource = getOptionalArgument<Long>()?.let { patientId ->
        ChatWithPatientDataSource(
            remoteRepository = remoteRepository,
            patientId = patientId,
        )
    } ?: ChatWithDoctorDataSource(
        remoteRepository = remoteRepository,
    )

    val loggedInUserId = loggedInUserIdProvider.loggedInUserId
        ?: return

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            ChatMessagingViewModel,
            ChatMessagingDataModel,
            ChatMessagingEvent,
            ChatMessagingEffect>(
        loop = Mobius.loop(
            Update<ChatMessagingDataModel, ChatMessagingEvent, ChatMessagingEffect> { model, event ->
                ChatMessagingLogic.update(
                    model,
                    event
                )
            },
            ChatMessagingLogic.effectHandler(
                messageDisplayer = messageDisplayer,
                resourceProvider = resourceProvider,
                dataSource = dataSource,
                messageInputClearer = {
                    lifecycleScope.launch {
                        clearMessageInput()
                    }
                },
            )
        )
            .logger(AndroidLogger.tag("ChatMessaging")),
        initialState = {
            ChatMessagingLogic.init(it)
        },
        defaultStateProvider = {
            ChatMessagingLogic.initialModel(
                loggedInUserId = loggedInUserId,
            )
        },
        modelMapper = {
            it.viewModel(
                resourceProvider = resourceProvider,
            )
        },
        render = this
    )

    val diffuserFragmentDelegate = DiffuserFragmentDelegate(this)

    eventSender = controllerFragmentDelegate
    diffuserProvider = { diffuserFragmentDelegate.diffuser }
    delegates = setOf(
        diffuserFragmentDelegate,
        controllerFragmentDelegate,
    )
}

private class ChatWithPatientDataSource(
    val remoteRepository: RemoteRepository,
    val patientId: Long,
) : ChatMessagingDataSource {

    override suspend fun getChatMessages(
        offset: Int,
        count: Int,
        sinceDate: LocalDateTime?
    ): Either<MessageListPortion, Throwable> {
        return remoteRepository.getChatWithPatientMessages(
            userId = patientId,
            offset = offset,
            count = count,
            sinceDate = sinceDate,
        )
    }

    override suspend fun sendChatMessage(
        message: String
    ): Either<Unit, Throwable> {
        return remoteRepository.sendMessageToChatWithPatient(
            userId = patientId,
            message = message,
        )
    }

    override suspend fun readChatMessages(
        until: LocalDateTime
    ): Either<Unit, Throwable> {
        return remoteRepository.readMessagesInChatWithPatient(
            userId = patientId,
            until = until,
        )
    }
}

private class ChatWithDoctorDataSource(
    val remoteRepository: RemoteRepository,
) : ChatMessagingDataSource {

    override suspend fun getChatMessages(
        offset: Int,
        count: Int,
        sinceDate: LocalDateTime?
    ): Either<MessageListPortion, Throwable> {
        return remoteRepository.getChatWithDoctorMessages(
            offset = offset,
            count = count,
            sinceDate = sinceDate,
        )
    }

    override suspend fun sendChatMessage(
        message: String
    ): Either<Unit, Throwable> {
        return remoteRepository.sendMessageToChatWithDoctor(
            message = message,
        )
    }

    override suspend fun readChatMessages(
        until: LocalDateTime
    ): Either<Unit, Throwable> {
        return remoteRepository.readMessagesInChatWithDoctor(
            until = until,
        )
    }
}