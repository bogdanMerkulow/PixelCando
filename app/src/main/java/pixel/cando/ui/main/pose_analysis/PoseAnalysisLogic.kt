package pixel.cando.ui.main.pose_analysis

import android.net.Uri
import android.os.Parcelable
import com.google.mlkit.vision.pose.Pose
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.PoseChecker
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight

object PoseAnalysisLogic {

    fun init(
        model: PoseAnalysisDataModel
    ): First<PoseAnalysisDataModel, PoseAnalysisEffect> {
        return First.first(
            model,
            setOf(
                PoseAnalysisEffect.CheckPose(
                    uri = model.uri,
                )
            )
        )
    }

    fun update(
        model: PoseAnalysisDataModel,
        event: PoseAnalysisEvent
    ): Next<PoseAnalysisDataModel, PoseAnalysisEffect> {
        return when (event) {
            // ui
            is PoseAnalysisEvent.ConfirmTap -> {
                Next.dispatch(
                    setOf(
                        PoseAnalysisEffect.Confirm(
                            uri = model.uri,
                        )
                    )
                )
            }
            is PoseAnalysisEvent.CancelTap -> {
                Next.dispatch(
                    setOf(
                        PoseAnalysisEffect.Exit
                    )
                )
            }
            is PoseAnalysisEvent.BackTap -> {
                Next.dispatch(
                    setOf(
                        PoseAnalysisEffect.Exit
                    )
                )
            }
            // model
            is PoseAnalysisEvent.MessageAfterPoseCheckReceived -> {
                Next.next(
                    model.copy(
                        message = event.message
                    )
                )
            }
        }
    }

    fun effectHandler(
        dismisser: () -> Unit,
        resultSender: (PoseAnalysisResult) -> Unit,
        poseChecker: PoseChecker,
    ): Connectable<PoseAnalysisEffect, PoseAnalysisEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PoseAnalysisEffect.CheckPose -> {
                    val result = poseChecker.check(effect.uri)
                    result.message?.let {
                        output.accept(
                            PoseAnalysisEvent.MessageAfterPoseCheckReceived(
                                message = it
                            )
                        )
                    }
                }
                is PoseAnalysisEffect.Confirm -> {
                    dismisser.invoke()
                    resultSender.invoke(
                        PoseAnalysisResult(
                            uri = effect.uri
                        )
                    )
                }
                is PoseAnalysisEffect.Exit -> {
                    dismisser.invoke()
                }
            }
        }

    fun initialModel(
        uri: Uri,
    ) = PoseAnalysisDataModel(
        uri = uri,
        message = null,
    )

}

sealed class PoseAnalysisEvent {
    // ui
    object ConfirmTap : PoseAnalysisEvent()
    object CancelTap : PoseAnalysisEvent()
    object BackTap : PoseAnalysisEvent()
    // model
    data class MessageAfterPoseCheckReceived(
        val message: String
    ): PoseAnalysisEvent()
}

sealed class PoseAnalysisEffect {
    data class Confirm(
        val uri: Uri,
    ) : PoseAnalysisEffect()

    data class CheckPose(
        val uri: Uri,
    ) : PoseAnalysisEffect()

    object Exit : PoseAnalysisEffect()
}

@Parcelize
data class PoseAnalysisDataModel(
    val uri: Uri,
    val message: String?,
) : Parcelable

data class PoseAnalysisViewModel(
    val uri: Uri,
    val message: String?,
)

fun PoseAnalysisDataModel.viewModel(
) = PoseAnalysisViewModel(
    uri = uri,
    message = message,
)

data class PoseAnalysisResult(
    val uri: Uri
)