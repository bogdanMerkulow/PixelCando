package pixel.cando.ui.main.patient_details

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.ExamListItemInfo
import pixel.cando.data.models.UploadPhotoFailure
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.di.PhotoPreviewArguments
import pixel.cando.ui.Screens
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
import java.time.format.DateTimeFormatter
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
                val weight = model.patientWeight
                val height = model.patientHeight
                if (weight != null
                    && height != null
                ) {
                    Next.dispatch(
                        setOf(
                            PatientDetailsEffect.AskToConfirmPhoto(
                                uri = event.uri,
                                weight = weight,
                                height = height,
                            )
                        )
                    )
                } else Next.noChange()
            }
            is PatientDetailsEvent.PhotoAccepted -> {
                Next.next(
                    model.copy(
                        isLoading = true,
                    ),
                    setOf(
                        PatientDetailsEffect.UploadPhoto(
                            patientId = model.patientId,
                            uri = event.uri,
                            weight = event.weight,
                            height = event.height,
                        )
                    )
                )
            }
            is PatientDetailsEvent.ExitTap -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.Exit
                    )
                )
            }
            is PatientDetailsEvent.ExamTap -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.NavigateToExamDetails(
                            examId = event.id,
                            patientId = model.patientId,
                        )
                    )
                )
            }
            is PatientDetailsEvent.PatientInfoTap -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.NavigateToPatientInfo(
                            patientId = model.patientId,
                        )
                    )
                )
            }
            // model
            is PatientDetailsEvent.LoadPatientInfoSuccess -> {
                Next.next(
                    model.copy(
                        patientFullName = event.patientFullName,
                        patientWeight = event.patientWeight,
                        patientHeight = event.patientHeight,
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
                        PatientDetailsEffect.CheckWriteStoragePermission
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
            is PatientDetailsEvent.WriteStoragePermissionGranted -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.OpenPhotoTaker
                    )
                )
            }
            is PatientDetailsEvent.WriteStoragePermissionDenied -> {
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
        photoConfirmationAsker: (PhotoPreviewArguments) -> Unit,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        cameraPermissionChecker: PermissionChecker,
        writeStoragePermissionChecker: PermissionChecker,
        flowRouter: FlowRouter,
        context: Context,
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
                                patientWeight = it.weight,
                                patientHeight = it.height,
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
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(
                            ImageDecoder.createSource(context.contentResolver, effect.uri)
                        )
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, effect.uri)
                    }
                    val base64 = bitmap.base64ForSending
                    if (base64 != null) {
                        val result = remoteRepository.uploadPhoto(
                            patientId = effect.patientId,
                            weight = effect.weight,
                            height = effect.height,
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
                    if (cameraPermissionChecker.checkPermission()) {
                        output.accept(
                            PatientDetailsEvent.CameraPermissionGranted
                        )
                    } else {
                        cameraPermissionChecker.requestPermission()
                    }
                }
                is PatientDetailsEffect.CheckWriteStoragePermission -> {
                    if (writeStoragePermissionChecker.checkPermission()) {
                        output.accept(
                            PatientDetailsEvent.WriteStoragePermissionGranted
                        )
                    } else {
                        writeStoragePermissionChecker.requestPermission()
                    }
                }
                is PatientDetailsEffect.AskToConfirmPhoto -> {
                    photoConfirmationAsker.invoke(
                        PhotoPreviewArguments(
                            uri = effect.uri,
                            weight = effect.weight,
                            height = effect.height,
                        )
                    )
                }
                is PatientDetailsEffect.NavigateToExamDetails -> {
                    flowRouter.navigateTo(
                        Screens.examDetails(
                            examId = effect.examId,
                            patientId = effect.patientId,
                        )
                    )
                }
                is PatientDetailsEffect.NavigateToPatientInfo -> {
                    flowRouter.navigateTo(
                        Screens.patientInfo(
                            patientId = effect.patientId,
                        )
                    )
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
        patientWeight = null,
        patientHeight = null,
        isLoading = false,
        listState = ParcelableListState.NotInitialized(),
    )

}

sealed class PatientDetailsEvent {
    // ui
    object RefreshRequest : PatientDetailsEvent()
    object LoadExamNextPage : PatientDetailsEvent()
    object PatientInfoTap : PatientDetailsEvent()

    object TakePhotoTap : PatientDetailsEvent()
    data class PhotoTaken(
        val uri: Uri
    ) : PatientDetailsEvent()

    object ExitTap : PatientDetailsEvent()

    data class PhotoAccepted(
        val uri: Uri,
        val weight: Float,
        val height: Float,
    ) : PatientDetailsEvent()

    data class ExamTap(
        val id: Long
    ) : PatientDetailsEvent()

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
        val patientWeight: Float,
        val patientHeight: Float,
    ) : PatientDetailsEvent()

    object PhotoUploadSuccess : PatientDetailsEvent()
    data class PhotoUploadFailure(
        val message: String? = null,
    ) : PatientDetailsEvent()

    object CameraPermissionGranted : PatientDetailsEvent()
    object CameraPermissionDenied : PatientDetailsEvent()

    object WriteStoragePermissionGranted : PatientDetailsEvent()
    object WriteStoragePermissionDenied : PatientDetailsEvent()
}

sealed class PatientDetailsEffect {

    data class LoadPatientInfo(
        val patientId: Long
    ) : PatientDetailsEffect()

    data class LoadExamPage(
        val patientId: Long,
        val page: Int,
    ) : PatientDetailsEffect()

    data class NavigateToExamDetails(
        val examId: Long,
        val patientId: Long,
    ) : PatientDetailsEffect()

    data class NavigateToPatientInfo(
        val patientId: Long,
    ) : PatientDetailsEffect()

    object OpenPhotoTaker : PatientDetailsEffect()

    data class UploadPhoto(
        val patientId: Long,
        val uri: Uri,
        val weight: Float,
        val height: Float,
    ) : PatientDetailsEffect()

    data class AskToConfirmPhoto(
        val uri: Uri,
        val weight: Float,
        val height: Float,
    ) : PatientDetailsEffect()

    object ShowUnexpectedError : PatientDetailsEffect()
    data class ShowErrorMessage(
        val message: String,
    ) : PatientDetailsEffect()

    object CheckCameraPermission : PatientDetailsEffect()

    object CheckWriteStoragePermission : PatientDetailsEffect()

    object Exit : PatientDetailsEffect()
}

@Parcelize
data class PatientDetailsDataModel(
    val patientId: Long,
    val patientFullName: String?,
    val patientWeight: Float?,
    val patientHeight: Float?,
    val isLoading: Boolean,
    val listState: ParcelableListState<ExamDataModel>,
) : Parcelable

@Parcelize
data class ExamDataModel(
    val id: Long,
    val createdAt: LocalDateTime,
    val number: Int,
    val weight: Float,
    val bmi: Float,
    val fatMass: Float,
    val fatFreeMass: Float,
    val abdominalFatMass: Float,
) : Parcelable

data class PatientDetailsViewModel(
    val title: String?,
    val isLoaderVisible: Boolean,
    val isTakePhotoButtonEnabled: Boolean,
    val listState: ListState<ExamViewModel>,
)

data class ExamViewModel(
    val id: Long,
    val weight: String,
    val fatMass: String,
    val fatFreeMass: String,
    val abdominalFatMass: String,
    val bmi: String,
    val date: String,
    val number: String,
    val isStarMarked: Boolean,
    val isFirst: Boolean,
    val isLast: Boolean,
)

fun PatientDetailsDataModel.viewModel(
    resourceProvider: ResourceProvider,
) = PatientDetailsViewModel(
    title = patientFullName,
    isLoaderVisible = isLoading,
    isTakePhotoButtonEnabled = isLoading.not(),
    listState = listState.plainState.map { exam, index, list ->
        val dateTimeFormatter = DateTimeFormatter
            .ofPattern("dd MMM, HH:mm")
            .withLocale(resourceProvider.getCurrentLocale())
        exam.viewModel(
            resourceProvider = resourceProvider,
            dateTimeFormatter = dateTimeFormatter,
            isFirst = index == 0,
            isLast = index == list.size - 1
        )
    }
)

private fun ExamDataModel.viewModel(
    resourceProvider: ResourceProvider,
    dateTimeFormatter: DateTimeFormatter,
    isFirst: Boolean,
    isLast: Boolean,
) = ExamViewModel(
    id = id,
    weight = "${resourceProvider.getString(R.string.weight)} $weight",
    fatMass = "${resourceProvider.getString(R.string.fat_mass)} $fatMass",
    fatFreeMass = "${resourceProvider.getString(R.string.fat_free_mass)} $fatFreeMass",
    abdominalFatMass = "${resourceProvider.getString(R.string.abdominal_fat_mass)} $abdominalFatMass",
    bmi = "${resourceProvider.getString(R.string.bmi)} $bmi",
    date = dateTimeFormatter.format(createdAt),
    number = number.toString(),
    isStarMarked = number == 1,
    isFirst = isFirst,
    isLast = isLast,
)

private fun ExamListItemInfo.dataModel(
) = ExamDataModel(
    id = id,
    createdAt = createdAt,
    number = number,
    weight = weight,
    fatMass = fatMass,
    fatFreeMass = fatFreeMass,
    abdominalFatMass = abdominalFatMass,
    bmi = bmi,
)