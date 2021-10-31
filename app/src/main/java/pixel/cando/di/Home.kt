package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.R
import pixel.cando.data.local.UserRoleStore
import pixel.cando.data.models.UserRole
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.home.HomeDataModel
import pixel.cando.ui.main.home.HomeEffect
import pixel.cando.ui.main.home.HomeEvent
import pixel.cando.ui.main.home.HomeFragment
import pixel.cando.ui.main.home.HomeLogic
import pixel.cando.ui.main.home.HomeTab
import pixel.cando.ui.main.home.HomeViewModel
import pixel.cando.ui.main.home.viewModel
import pixel.cando.ui.main.patient_flow.PatientFlowFragment
import pixel.cando.ui.main.photo_list.PhotoListFragment
import pixel.cando.ui.main.profile.ProfileFlowFragment
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate

fun HomeFragment.setup(
    userRoleStore: UserRoleStore,
) {
    val userRole = userRoleStore.userRole
        ?: return
    if (tabs.isEmpty()) {
        tabs = when (userRole) {
            UserRole.DOCTOR -> {
                listOf(
                    HomeTab(
                        title = R.string.tab_title_patients,
                        icon = R.drawable.ic_patients,
                        fragmentProvider = { PatientFlowFragment() },
                    ),
                    HomeTab(
                        title = R.string.tab_title_profile,
                        icon = R.drawable.ic_user,
                        fragmentProvider = { ProfileFlowFragment() },
                    ),
                )
            }
            UserRole.PATIENT -> {
                listOf(
                    HomeTab(
                        title = R.string.tab_title_photos,
                        icon = R.drawable.ic_photo_library,
                        fragmentProvider = { PhotoListFragment() },
                    ),
                    HomeTab(
                        title = R.string.tab_title_profile,
                        icon = R.drawable.ic_user,
                        fragmentProvider = { ProfileFlowFragment() },
                    ),
                )
            }
        }
    }
    if (delegates.isEmpty()) {
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

}