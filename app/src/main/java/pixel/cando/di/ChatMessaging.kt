package pixel.cando.di

import androidx.lifecycle.lifecycleScope
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.data.local.LoggedInUserIdProvider
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.chat_messaging.ChatMessagingDataModel
import pixel.cando.ui.main.chat_messaging.ChatMessagingEffect
import pixel.cando.ui.main.chat_messaging.ChatMessagingEvent
import pixel.cando.ui.main.chat_messaging.ChatMessagingFragment
import pixel.cando.ui.main.chat_messaging.ChatMessagingLogic
import pixel.cando.ui.main.chat_messaging.ChatMessagingViewModel
import pixel.cando.ui.main.chat_messaging.viewModel
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun ChatMessagingFragment.setup(
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    loggedInUserIdProvider: LoggedInUserIdProvider,
    flowRouter: FlowRouter,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val chatId = getArgument<Long>()

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
                remoteRepository = remoteRepository,
                flowRouter = flowRouter,
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
                chatId = chatId,
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