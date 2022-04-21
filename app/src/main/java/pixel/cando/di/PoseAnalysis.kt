package pixel.cando.di

import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.coroutines.launch
import pixel.cando.ui._base.fragment.FragmentDelegate
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui._base.tea.ResultEmitter
import pixel.cando.ui.main.pose_analysis.PoseAnalysisDataModel
import pixel.cando.ui.main.pose_analysis.PoseAnalysisEffect
import pixel.cando.ui.main.pose_analysis.PoseAnalysisEvent
import pixel.cando.ui.main.pose_analysis.PoseAnalysisFragment
import pixel.cando.ui.main.pose_analysis.PoseAnalysisLogic
import pixel.cando.ui.main.pose_analysis.PoseAnalysisResult
import pixel.cando.ui.main.pose_analysis.PoseAnalysisViewModel
import pixel.cando.ui.main.pose_analysis.viewModel
import pixel.cando.utils.PoseChecker
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

fun PoseAnalysisFragment.setup(
    poseChecker: PoseChecker
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val uri = getArgument<Uri>()

    val dependencies = this.findDelegateOrThrow<PoseAnalysisDependencies>()

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            PoseAnalysisViewModel,
            PoseAnalysisDataModel,
            PoseAnalysisEvent,
            PoseAnalysisEffect>(
        loop = Mobius.loop(
            Update<PoseAnalysisDataModel, PoseAnalysisEvent, PoseAnalysisEffect> { model, event ->
                PoseAnalysisLogic.update(
                    model,
                    event
                )
            },
            PoseAnalysisLogic.effectHandler(
                dismisser = {
                    lifecycleScope.launch {
                        dismiss()
                    }
                },
                resultSender = {
                    dependencies.resultEmitter.emit(it)
                },
                poseChecker = poseChecker,
            )
        )
            .logger(AndroidLogger.tag("PoseAnalysis")),
        initialState = {
            PoseAnalysisLogic.init(it)
        },
        defaultStateProvider = {
            PoseAnalysisLogic.initialModel(
                uri = uri,
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

interface PoseAnalysisDependencies : FragmentDelegate {
    val resultEmitter: ResultEmitter<PoseAnalysisResult>
}