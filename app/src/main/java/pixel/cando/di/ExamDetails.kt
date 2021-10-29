package pixel.cando.di

import android.os.Parcelable
import com.spotify.mobius.Mobius
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import kotlinx.parcelize.Parcelize
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.fragment.getArgument
import pixel.cando.ui._base.tea.ControllerFragmentDelegate
import pixel.cando.ui.main.exam_details.ExamDetailsDataModel
import pixel.cando.ui.main.exam_details.ExamDetailsEffect
import pixel.cando.ui.main.exam_details.ExamDetailsEvent
import pixel.cando.ui.main.exam_details.ExamDetailsFragment
import pixel.cando.ui.main.exam_details.ExamDetailsLogic
import pixel.cando.ui.main.exam_details.ExamDetailsViewModel
import pixel.cando.ui.main.exam_details.viewModel
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.diffuser.DiffuserFragmentDelegate
import pixel.cando.utils.messageDisplayer

@Parcelize
data class ExamDetailsArgument(
    val examId: Long,
    val patientId: Long,
) : Parcelable

fun ExamDetailsFragment.setup(
    resourceProvider: ResourceProvider,
    remoteRepository: RemoteRepository,
    flowRouter: FlowRouter,
) {
    if (delegates.isNotEmpty()) {
        return
    }

    val argument = getArgument<ExamDetailsArgument>()

    val controllerFragmentDelegate = ControllerFragmentDelegate<
            ExamDetailsViewModel,
            ExamDetailsDataModel,
            ExamDetailsEvent,
            ExamDetailsEffect>(
        loop = Mobius.loop(
            Update<ExamDetailsDataModel, ExamDetailsEvent, ExamDetailsEffect> { model, event ->
                ExamDetailsLogic.update(
                    model,
                    event
                )
            },
            ExamDetailsLogic.effectHandler(
                messageDisplayer = messageDisplayer,
                resourceProvider = resourceProvider,
                remoteRepository = remoteRepository,
                flowRouter = flowRouter,
            )
        )
            .logger(AndroidLogger.tag("ExamDetails")),
        initialState = {
            ExamDetailsLogic.init(it)
        },
        defaultStateProvider = {
            ExamDetailsLogic.initialModel(
                examId = argument.examId,
                patientId = argument.patientId,
            )
        },
        modelMapper = {
            it.viewModel(
                resourceProvider = resourceProvider,
            )
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