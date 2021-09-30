package pixel.cando.ui.main.patient_details

import android.Manifest
import android.graphics.Bitmap
import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.Exam
import pixel.cando.data.models.UploadPhotoFailure
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.list.ListAction
import pixel.cando.ui._base.list.ListState
import pixel.cando.ui._base.list.ParcelableListState
import pixel.cando.ui._base.list.isLoading
import pixel.cando.ui._base.list.listStateUpdater
import pixel.cando.ui._base.list.map
import pixel.cando.ui._base.list.plainState
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui._base.tea.toFirst
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.PermissionChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.base64ForSending
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

object PatientDetailsLogic {

    fun init(
        model: PatientDetailsDataModel
    ): First<PatientDetailsDataModel, PatientDetailsEffect> {
        return when {
            model.listState is ParcelableListState.NotInitialized -> {
                val next = listUpdater.update(
                    model,
                    PatientDetailsEvent.RefreshRequest
                )
                First.first(
                    next.modelUnsafe(),
                    next.effects()
                            + setOf(
                        PatientDetailsEffect.LoadPatientInfo(
                            patientId = model.patientId,
                        )
                    )
                )
            }
            model.listState.isLoading -> {
                listUpdater.update(
                    model,
                    PatientDetailsEvent.StopExamListLoading
                ).toFirst
            }
            else -> {
                First.first(
                    model
                )
            }
        }
    }

    fun update(
        model: PatientDetailsDataModel,
        event: PatientDetailsEvent
    ): Next<PatientDetailsDataModel, PatientDetailsEffect> {
        return when (event) {
            // ui
            is PatientDetailsEvent.RefreshRequest,
            is PatientDetailsEvent.ExamListLoadSuccess,
            is PatientDetailsEvent.ExamListLoadFailure,
            is PatientDetailsEvent.LoadExamNextPage,
            is PatientDetailsEvent.StopExamListLoading -> {
                listUpdater.update(
                    model,
                    event
                )
            }
            is PatientDetailsEvent.TakePhotoTap -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.CheckCameraPermission
                    )
                )
            }
            is PatientDetailsEvent.PhotoTaken -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.AskToConfirmPhoto(
                            bitmap = event.bitmap
                        )
                    )
                )
            }
            is PatientDetailsEvent.PhotoAccepted -> {
                Next.next(
                    model.copy(
                        isLoading = true,
                    ),
                    setOf(
                        PatientDetailsEffect.UploadPhoto(
                            patientId = model.patientId,
                            bitmap = event.bitmap
                        )
                    )
                )
            }
            is PatientDetailsEvent.PhotoDeclined -> {
                Next.noChange()
            }
            is PatientDetailsEvent.ExitTap -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.Exit
                    )
                )
            }
            // model
            is PatientDetailsEvent.LoadPatientInfoSuccess -> {
                Next.next(
                    model.copy(
                        patientFullName = event.patientFullName,
                    )
                )
            }
            is PatientDetailsEvent.PhotoUploadSuccess -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    )
                )
            }
            is PatientDetailsEvent.PhotoUploadFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        if (event.message.isNullOrBlank()) PatientDetailsEffect.ShowUnexpectedError
                        else PatientDetailsEffect.ShowErrorMessage(event.message)
                    )
                )
            }
            is PatientDetailsEvent.CameraPermissionGranted -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.OpenPhotoTaker
                    )
                )
            }
            is PatientDetailsEvent.CameraPermissionDenied -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.ShowUnexpectedError // TODO change the message
                    )
                )
            }
        }
    }

    private val listUpdater = listStateUpdater<
            PatientDetailsDataModel,
            PatientDetailsEvent,
            PatientDetailsEffect,
            ExamDataModel>(
        listStateExtractor = { listState },
        eventMapper = {
            when (it) {
                is PatientDetailsEvent.RefreshRequest -> ListAction.Refresh()
                is PatientDetailsEvent.ExamListLoadSuccess -> {
                    if (it.exams.isNotEmpty()) ListAction.PageLoaded(it.exams)
                    else ListAction.EmptyPageLoaded()
                }
                is PatientDetailsEvent.ExamListLoadFailure -> ListAction.PageLoadFailed(it.error)
                is PatientDetailsEvent.LoadExamNextPage -> ListAction.LoadMore()
                is PatientDetailsEvent.StopExamListLoading -> ListAction.StopLoading()
                else -> null
            }
        },
        modelUpdater = { copy(listState = it) },
        loadPageEffectMapper = {
            PatientDetailsEffect.LoadExamPage(
                patientId = patientId,
                page = it.page,
            )
        },
        emitErrorEffectMapper = {
            PatientDetailsEffect.ShowErrorMessage(
                it.error.message ?: it.error.localizedMessage
            )
        }
    )

    fun effectHandler(
        photoTakerOpener: () -> Unit,
        photoConfirmationAsker: (Bitmap) -> Unit,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        permissionChecker: PermissionChecker,
        flowRouter: FlowRouter,
    ): Connectable<PatientDetailsEffect, PatientDetailsEvent> {
        val loadExamPageJob = AtomicReference<Job>()
        return CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PatientDetailsEffect.LoadPatientInfo -> {
                    val result = remoteRepository.getPatient(
                        patientId = effect.patientId
                    )
                    result.onLeft {
                        output.accept(
                            PatientDetailsEvent.LoadPatientInfoSuccess(
                                patientFullName = it.fullName,
                            )
                        )
                    }
                }
                is PatientDetailsEffect.LoadExamPage -> {
                    loadExamPageJob.getAndSet(
                        launch {
                            val result = remoteRepository.getExams(
                                patientId = effect.patientId,
                                page = effect.page,
                            )
                            result.onLeft {
                                output.accept(
                                    PatientDetailsEvent.ExamListLoadSuccess(
                                        it.map { it.dataModel() }
                                    )
                                )
                            }
                            result.onRight {
                                output.accept(
                                    PatientDetailsEvent.ExamListLoadFailure(it)
                                )
                            }
                        }
                    )?.cancel()
                }
                is PatientDetailsEffect.OpenPhotoTaker -> {
                    photoTakerOpener.invoke()
                }
                is PatientDetailsEffect.UploadPhoto -> {
                    val base64 = effect.bitmap.base64ForSending
                    if (base64 != null) {
                        val result = remoteRepository.uploadPhoto(
                            patientId = effect.patientId,
                            photo = base64
                        )
                        result.onLeft {
                            output.accept(
                                PatientDetailsEvent.PhotoUploadSuccess
                            )
                        }
                        result.onRight {
                            when (it) {
                                is UploadPhotoFailure.ErrorMessage -> {
                                    output.accept(
                                        PatientDetailsEvent.PhotoUploadFailure(
                                            message = it.message
                                        )
                                    )
                                }
                                is UploadPhotoFailure.UnknownError -> {
                                    logError(it.throwable)
                                    output.accept(
                                        PatientDetailsEvent.PhotoUploadFailure()
                                    )
                                }
                            }
                        }
                    } else {
                        output.accept(
                            PatientDetailsEvent.PhotoUploadFailure()
                        )
                    }
                }
                is PatientDetailsEffect.CheckCameraPermission -> {
                    val permission = Manifest.permission.CAMERA
                    if (permissionChecker.checkPermission(permission)) {
                        output.accept(
                            PatientDetailsEvent.CameraPermissionGranted
                        )
                    } else {
                        permissionChecker.requestPermission(permission)
                    }
                }
                is PatientDetailsEffect.AskToConfirmPhoto -> {
                    photoConfirmationAsker.invoke(effect.bitmap)
                }
                is PatientDetailsEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
                is PatientDetailsEffect.ShowErrorMessage -> {
                    messageDisplayer.showMessage(
                        effect.message
                    )
                }
                is PatientDetailsEffect.Exit -> {
                    flowRouter.exit()
                }
            }
        }
    }

    fun initialModel(
        patientId: Long,
    ) = PatientDetailsDataModel(
        patientId = patientId,
        patientFullName = null,
        isLoading = false,
        listState = ParcelableListState.NotInitialized(),
    )

}

sealed class PatientDetailsEvent {
    // ui
    object RefreshRequest : PatientDetailsEvent()
    object LoadExamNextPage : PatientDetailsEvent()

    object TakePhotoTap : PatientDetailsEvent()
    data class PhotoTaken(
        val bitmap: Bitmap
    ) : PatientDetailsEvent()

    object ExitTap : PatientDetailsEvent()

    data class PhotoAccepted(
        val bitmap: Bitmap
    ) : PatientDetailsEvent()

    object PhotoDeclined : PatientDetailsEvent()

    // model

    class ExamListLoadSuccess(
        val exams: List<ExamDataModel>,
    ) : PatientDetailsEvent()

    class ExamListLoadFailure(
        val error: Throwable,
    ) : PatientDetailsEvent()

    object StopExamListLoading : PatientDetailsEvent()

    data class LoadPatientInfoSuccess(
        val patientFullName: String,
    ) : PatientDetailsEvent()

    object PhotoUploadSuccess : PatientDetailsEvent()
    data class PhotoUploadFailure(
        val message: String? = null,
    ) : PatientDetailsEvent()

    object CameraPermissionGranted : PatientDetailsEvent()
    object CameraPermissionDenied : PatientDetailsEvent()
}

sealed class PatientDetailsEffect {

    data class LoadPatientInfo(
        val patientId: Long
    ) : PatientDetailsEffect()

    data class LoadExamPage(
        val patientId: Long,
        val page: Int,
    ) : PatientDetailsEffect()

    object OpenPhotoTaker : PatientDetailsEffect()

    data class UploadPhoto(
        val patientId: Long,
        val bitmap: Bitmap
    ) : PatientDetailsEffect()

    data class AskToConfirmPhoto(
        val bitmap: Bitmap
    ) : PatientDetailsEffect()

    object ShowUnexpectedError : PatientDetailsEffect()
    data class ShowErrorMessage(
        val message: String,
    ) : PatientDetailsEffect()

    object CheckCameraPermission : PatientDetailsEffect()

    object Exit : PatientDetailsEffect()
}

@Parcelize
data class PatientDetailsDataModel(
    val patientId: Long,
    val patientFullName: String?,
    val isLoading: Boolean,
    val listState: ParcelableListState<ExamDataModel>,
) : Parcelable

@Parcelize
data class ExamDataModel(
    val id: Long,
    val creationDate: LocalDateTime,
    val number: Int,
    val bmi: Float,
) : Parcelable

data class PatientDetailsViewModel(
    val title: String,
    val isLoaderVisible: Boolean,
    val isTakePhotoButtonEnabled: Boolean,
    val listState: ListState<ExamViewModel>,
)

data class ExamViewModel(
    val id: Long,
    val value: String,
    val date: String,
    val number: String,
    val isStarMarked: Boolean,
    val isFirst: Boolean,
    val isLast: Boolean,
)

fun PatientDetailsDataModel.viewModel(
    resourceProvider: ResourceProvider,
) = PatientDetailsViewModel(
    title = patientFullName ?: "",
    isLoaderVisible = isLoading,
    isTakePhotoButtonEnabled = isLoading.not(),
    listState = listState.plainState.map { exam, index, list ->
        exam.viewModel(
            resourceProvider = resourceProvider,
            isFirst = index == 0,
            isLast = index == list.size - 1
        )
    }
)

private fun ExamDataModel.viewModel(
    resourceProvider: ResourceProvider,
    isFirst: Boolean,
    isLast: Boolean,
) = ExamViewModel(
    id = id,
    value = resourceProvider.getString(R.string.exam_bmi_value, bmi),
    date = resourceProvider.getQuantityString(
        R.plurals.days_ago,
        creationDate.until(LocalDateTime.now(), ChronoUnit.DAYS).toInt(),
    ),
    number = number.toString(),
    isStarMarked = number == 1,
    isFirst = isFirst,
    isLast = isLast,
)

private fun Exam.dataModel(
) = ExamDataModel(
    id = id,
    creationDate = createdAt,
    number = number,
    bmi = bmi,
)