package pixel.cando.ui.main.photo_preview

import android.net.Uri
import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler

object PhotoPreviewLogic {

    fun init(
        model: PhotoPreviewDataModel
    ): First<PhotoPreviewDataModel, PhotoPreviewEffect> {
        return First.first(model)
    }

    fun update(
        model: PhotoPreviewDataModel,
        event: PhotoPreviewEvent
    ): Next<PhotoPreviewDataModel, PhotoPreviewEffect> {
        return when (event) {
            is PhotoPreviewEvent.ConfirmTap -> {
                Next.dispatch(
                    setOf(
                        PhotoPreviewEffect.Confirm(
                            uri = model.uri,
                            weight = model.weight,
                            height = model.height,
                        )
                    )
                )
            }
            is PhotoPreviewEvent.WeightChanged -> {
                Next.next(
                    model.copy(
                        weight = event.weight.toFloatOrNull() ?: model.weight
                    )
                )
            }
            is PhotoPreviewEvent.CancelTap,
            is PhotoPreviewEvent.BackTap -> {
                Next.dispatch(
                    setOf(
                        PhotoPreviewEffect.Exit
                    )
                )
            }
        }
    }

    fun effectHandler(
        dismisser: () -> Unit,
        resultSender: (PhotoPreviewResult) -> Unit,
    ): Connectable<PhotoPreviewEffect, PhotoPreviewEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PhotoPreviewEffect.Confirm -> {
                    dismisser.invoke()
                    resultSender.invoke(
                        PhotoPreviewResult(
                            uri = effect.uri,
                            weight = effect.weight,
                            height = effect.height,
                        )
                    )
                }
                is PhotoPreviewEffect.Exit -> {
                    dismisser.invoke()
                }
            }
        }

    fun initialModel(
        uri: Uri,
        weight: Float,
        height: Float,
    ) = PhotoPreviewDataModel(
        uri = uri,
        weight = weight,
        height = height,
    )

}

sealed class PhotoPreviewEvent {
    object ConfirmTap : PhotoPreviewEvent()
    object CancelTap : PhotoPreviewEvent()
    object BackTap : PhotoPreviewEvent()

    data class WeightChanged(
        val weight: String
    ) : PhotoPreviewEvent()

}

sealed class PhotoPreviewEffect {
    data class Confirm(
        val uri: Uri,
        val weight: Float,
        val height: Float
    ) : PhotoPreviewEffect()

    object Exit : PhotoPreviewEffect()
}

@Parcelize
data class PhotoPreviewDataModel(
    val uri: Uri,
    val weight: Float,
    val height: Float
) : Parcelable

data class PhotoPreviewViewModel(
    val uri: Uri,
    val weight: String,
    val height: String
)

fun PhotoPreviewDataModel.viewModel(
) = PhotoPreviewViewModel(
    uri = uri,
    weight = weight.toString(),
    height = height.toString(),
)

data class PhotoPreviewResult(
    val uri: Uri,
    val weight: Float,
    val height: Float
)