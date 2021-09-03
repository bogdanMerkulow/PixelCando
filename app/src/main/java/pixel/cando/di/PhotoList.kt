package pixel.cando.di

import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.photo_list.*
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate

fun setup(
    fragment: PhotoListFragment
) {
    if (fragment.delegates.isNotEmpty()) {
        return
    }
    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PhotoListViewModel,
            PhotoListDataModel,
            PhotoListEvent,
            PhotoListEffect>(
        loop = Mobius.loop(
            Update<PhotoListDataModel, PhotoListEvent, PhotoListEffect> { model, event ->
                PhotoListLogic.update(
                    model,
                    event
                )
            },
            PhotoListLogic.effectHandler(
            )
        )
            .logger(AndroidLogger.tag("PhotoList")),
        initialState = {
            PhotoListLogic.init(it)
        },
        defaultStateProvider = {
            PhotoListLogic.initialModel()
        },
        modelMapper = {
            it.viewModel()
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