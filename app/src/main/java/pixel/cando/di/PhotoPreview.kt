package pixel.cando.di

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.lifecycleScope
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.ui._base.fragment.FragmentDelegate
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui._base.tea.ResultEmitter
import pixel.cando.ui.main.photo_preview.PhotoPreviewDataModel
import pixel.cando.ui.main.photo_preview.PhotoPreviewEffect
import pixel.cando.ui.main.photo_preview.PhotoPreviewEvent
import pixel.cando.ui.main.photo_preview.PhotoPreviewFragment
import pixel.cando.ui.main.photo_preview.PhotoPreviewLogic
import pixel.cando.ui.main.photo_preview.PhotoPreviewResult
import pixel.cando.ui.main.photo_preview.PhotoPreviewViewModel
import pixel.cando.ui.main.photo_preview.viewModel
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate

@Parcelize
data class PhotoPreviewArguments(
    val uri: Uri,
    val weight: Float,
    val weightUnit: String,
    val height: String,
) : Parcelable

fun PhotoPreviewFragment.setup(
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val arguments: PhotoPreviewArguments = getArgument()

    val dependencies = this.findDelegateOrThrow<PhotoPreviewDependencies>()

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PhotoPreviewViewModel,
            PhotoPreviewDataModel,
            PhotoPreviewEvent,
            PhotoPreviewEffect>(
        loop = Mobius.loop(
            Update<PhotoPreviewDataModel, PhotoPreviewEvent, PhotoPreviewEffect> { model, event ->
                PhotoPreviewLogic.update(
                    model,
                    event
                )
            },
            PhotoPreviewLogic.effectHandler(
                dismisser = {
                    lifecycleScope.launch {
                        dismiss()
                    }
                },
                resultSender = {
                    dependencies.resultEmitter.emit(it)
                }
            )
        )
            .logger(AndroidLogger.tag("PhotoPreview")),
        initialState = {
            PhotoPreviewLogic.init(it)
        },
        defaultStateProvider = {
            PhotoPreviewLogic.initialModel(
                uri = arguments.uri,
                height = arguments.height,
                weight = arguments.weight,
                weightUnit = arguments.weightUnit
            )
        },
        modelMapper = {
            it.viewModel()
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

interface PhotoPreviewDependencies : FragmentDelegate {
    val resultEmitter: ResultEmitter<PhotoPreviewResult>
}