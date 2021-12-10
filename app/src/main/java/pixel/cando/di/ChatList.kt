package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.data.local.LoggedInUserIdProvider
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.chat_list.ChatListDataModel
import pixel.cando.ui.main.chat_list.ChatListEffect
import pixel.cando.ui.main.chat_list.ChatListEvent
import pixel.cando.ui.main.chat_list.ChatListFragment
import pixel.cando.ui.main.chat_list.ChatListLogic
import pixel.cando.ui.main.chat_list.ChatListViewModel
import pixel.cando.ui.main.chat_list.viewModel
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun ChatListFragment.setup(
    remoteRepository: RemoteRepository,
    resourceProvider: ResourceProvider,
    loggedInUserIdProvider: LoggedInUserIdProvider,
    flowRouter: FlowRouter,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val loggedInUserId = loggedInUserIdProvider.loggedInUserId
        ?: return

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            ChatListViewModel,
            ChatListDataModel,
            ChatListEvent,
            ChatListEffect>(
        loop = Mobius.loop(
            Update<ChatListDataModel, ChatListEvent, ChatListEffect> { model, event ->
                ChatListLogic.update(
                    model,
                    event
                )
            },
            ChatListLogic.effectHandler(
                messageDisplayer = messageDisplayer,
                resourceProvider = resourceProvider,
                remoteRepository = remoteRepository,
                flowRouter = flowRouter,
            )
        )
            .logger(AndroidLogger.tag("ChatList")),
        initialState = {
            ChatListLogic.init(it)
        },
        defaultStateProvider = {
            ChatListLogic.initialModel(
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