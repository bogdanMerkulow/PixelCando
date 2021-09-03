package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.R
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.models.UserRole
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.chat.ChatFragment
import pixel.cando.ui.main.chat_list.ChatListFragment
import pixel.cando.ui.main.home.*
import pixel.cando.ui.main.patient_list.PatientListFragment
import pixel.cando.ui.main.photo_list.PhotoListFragment
import pixel.cando.ui.main.profile.ProfileFragment
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate

fun setup(
    fragment: HomeFragment,
    userRoleStore: UserRoleStore,
    resourceProvider: ResourceProvider,
) {
    val userRole = userRoleStore.userRole
        ?: return
    if (fragment.tabs.isEmpty()) {
        fragment.tabs = when (userRole) {
            UserRole.DOCTOR -> {
                listOf(
                    HomeTab(
                        title = R.string.tab_title_chats,
                        icon = R.drawable.ic_chat,
                        fragmentProvider = { ChatListFragment() },
                    ),
                    HomeTab(
                        title = R.string.tab_title_patients,
                        icon = R.drawable.ic_patients,
                        fragmentProvider = { PatientListFragment() },
                    ),
                    HomeTab(
                        title = R.string.tab_title_profile,
                        icon = R.drawable.ic_user,
                        fragmentProvider = { ProfileFragment() },
                    ),
                )
            }
            UserRole.PATIENT -> {
                listOf(
                    HomeTab(
                        title = R.string.tab_title_chat,
                        icon = R.drawable.ic_chat,
                        fragmentProvider = { ChatFragment() },
                    ),
                    HomeTab(
                        title = R.string.tab_title_photos,
                        icon = R.drawable.ic_photo_library,
                        fragmentProvider = { PhotoListFragment() },
                    ),
                    HomeTab(
                        title = R.string.tab_title_profile,
                        icon = R.drawable.ic_user,
                        fragmentProvider = { ProfileFragment() },
                    ),
                )
            }
        }
    }
    if (fragment.delegates.isEmpty()) {
        val controllerFragmentDelegate = ControllerFragmentDelegate<
                HomeViewModel,
                HomeDataModel,
                HomeEvent,
                HomeEffect>(
            loop = Mobius.loop(
                Update<HomeDataModel, HomeEvent, HomeEffect> { model, event ->
                    HomeLogic.update(
                        model,
                        event
                    )
                },
                HomeLogic.effectHandler()
            )
                .logger(AndroidLogger.tag("Home")),
            initialState = {
                HomeLogic.init(it)
            },
            defaultStateProvider = {
                HomeLogic.initialModel()
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

}