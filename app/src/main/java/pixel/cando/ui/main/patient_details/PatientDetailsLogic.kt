package pixel.cando.ui.main.patient_details

import android.content.Context
import android.net.Uri
import android.os.Parcelable
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
import pixel.cando.ui._base.tea.mapEffects
import pixel.cando.ui._base.tea.toFirst
import pixel.cando.ui.main.patient_photo_review.PatientPhotoReviewArguments
import pixel.cando.utils.ImagePicker
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.PermissionChecker
import pixel.cando.utils.PoseChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.TimeAgoFormatter
import pixel.cando.utils.base64
import pixel.cando.utils.handleSkippingCancellation
import pixel.cando.utils.loadReducedBitmap
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
                        .plus(
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
                ).toFirst(model)
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
                ).mapEffects {
                    if (event is PatientDetailsEvent.RefreshRequest)
                        it.plus(
                            PatientDetailsEffect.LoadPatientInfo(
                                patientId = model.patientId,
                            )
                        )
                    else it
                }
            }
            is PatientDetailsEvent.CreateExamTap -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.AskHowToGetPhoto
                    )
                )
            }
            is PatientDetailsEvent.PhotoTakingChosen -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.CheckCameraPermission
                    )
                )
            }
            is PatientDetailsEvent.ImagePickingChosen -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.OpenImagePicker
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
            is PatientDetailsEvent.ReviewPatientTap -> {
                val patientData = model.patientData
                if (model.isLoading.not()
                    && patientData?.photoToReview != null
                ) {
                    Next.dispatch(
                        setOf(
                            PatientDetailsEffect.NavigateToPatientPhotoReview(
                                patientFullName = patientData.fullName,
                                photoUrl = patientData.photoToReview.url,
                            )
                        )
                    )
                } else Next.noChange()
            }
            is PatientDetailsEvent.PhotoConfirmed -> {
                val photoId = model.patientData?.photoToReview?.id
                if (photoId != null) {
                    Next.next(
                        model.copy(
                            isLoading = true,
                        ),
                        setOf(
                            PatientDetailsEffect.ConfirmPhoto(photoId)
                        )
                    )
                } else Next.noChange()
            }
            is PatientDetailsEvent.PhotoRejected -> {
                val photoId = model.patientData?.photoToReview?.id
                if (photoId != null) {
                    Next.next(
                        model.copy(
                            isLoading = true,
                        ),
                        setOf(
                            PatientDetailsEffect.RejectPhoto(
                                id = photoId,
                                reason = event.reason,
                            )
                        )
                    )
                } else Next.noChange()
            }
            // model
            is PatientDetailsEvent.LoadPatientInfoSuccess -> {
                Next.next(
                    model.copy(
                        patientData = event.patientData,
                        isLoading = false,
                    )
                )
            }
            is PatientDetailsEvent.PhotoUploadSuccess -> {
                listUpdater.update(
                    model.copy(
                        isLoading = false,
                    ),
                    PatientDetailsEvent.RefreshRequest
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
            is PatientDetailsEvent.PhotoTaken -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.CheckPoseInPhoto(
                            uri = event.uri,
                        )
                    )
                )
            }
            is PatientDetailsEvent.ImagePicked -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.CheckPoseInPhoto(
                            uri = event.uri,
                        )
                    )
                )
            }
            is PatientDetailsEvent.PoseInPhotoChecked -> {
                val patientData = model.patientData
                if (patientData != null) {
                    Next.dispatch(
                        setOf(
                            PatientDetailsEffect.AskToConfirmPhoto(
                                uri = event.uri,
                                weight = patientData.weight,
                                height = patientData.height,
                            )
                        )
                    )
                } else Next.noChange()
            }
            is PatientDetailsEvent.ConfirmPhotoSuccess -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                        patientData = model.patientData?.copy(
                            photoToReview = null
                        )
                    ),
                    setOf(
                        PatientDetailsEffect.LoadPatientInfo(
                            model.patientId
                        )
                    )
                )
            }
            is PatientDetailsEvent.ConfirmPhotoFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        PatientDetailsEffect.ShowUnexpectedError
                    )
                )
            }
            is PatientDetailsEvent.RejectPhotoSuccess -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                        patientData = model.patientData?.copy(
                            photoToReview = null
                        )
                    ),
                    setOf(
                        PatientDetailsEffect.LoadPatientInfo(
                            model.patientId
                        )
                    )
                )
            }
            is PatientDetailsEvent.RejectPhotoFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        PatientDetailsEffect.ShowUnexpectedError
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
        howToGetPhotoAsker: () -> Unit,
        patientPhotoReviewOpener: (PatientPhotoReviewArguments) -> Unit,
        poseAnalyserOpener: (Uri) -> Unit,
        poseChecker: PoseChecker,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        cameraPermissionChecker: PermissionChecker,
        writeStoragePermissionChecker: PermissionChecker,
        imagePicker: ImagePicker,
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
                                PatientLoadableDataModel(
                                    fullName = it.fullName,
                                    weight = it.weight,
                                    height = it.height,
                                    photoToReview = it.photoToReview?.let {
                                        PatientPhotoToReviewDataModel(
                                            id = it.id,
                                            createdAt = it.createdAt,
                                            url = it.url,
                                        )
                                    }
                                )

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
                is PatientDetailsEffect.OpenImagePicker -> {
                    imagePicker.pickImage()
                }
                is PatientDetailsEffect.UploadPhoto -> {
                    try {
                        val bitmap = context.loadReducedBitmap(effect.uri)
                        val base64 = bitmap.base64
                        bitmap.recycle()
                        val result = remoteRepository.uploadPhotoByDoctor(
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
                    } catch (t: Throwable) {
                        t.handleSkippingCancellation {
                            logError(t)
                            output.accept(
                                PatientDetailsEvent.PhotoUploadFailure()
                            )
                        }
                    }
                }
                is PatientDetailsEffect.ConfirmPhoto -> {
                    val result = remoteRepository.confirmPhoto(effect.id)
                    result.onLeft {
                        output.accept(
                            PatientDetailsEvent.ConfirmPhotoSuccess
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            PatientDetailsEvent.ConfirmPhotoSuccess
                        )
                    }
                }
                is PatientDetailsEffect.RejectPhoto -> {
                    val result = remoteRepository.rejectPhoto(
                        id = effect.id,
                        reason = effect.reason,
                    )
                    result.onLeft {
                        output.accept(
                            PatientDetailsEvent.RejectPhotoSuccess
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            PatientDetailsEvent.RejectPhotoFailure
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
                is PatientDetailsEffect.CheckPoseInPhoto -> {
                    val result = poseChecker.check(effect.uri)
                    if (result.success) {
                        output.accept(
                            PatientDetailsEvent.PoseInPhotoChecked(
                                uri = effect.uri
                            )
                        )
                    } else {
                        poseAnalyserOpener.invoke(effect.uri)
                    }
                }
                is PatientDetailsEffect.AskHowToGetPhoto -> {
                    howToGetPhotoAsker.invoke()
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
                is PatientDetailsEffect.NavigateToPatientPhotoReview -> {
                    patientPhotoReviewOpener.invoke(
                        PatientPhotoReviewArguments(
                            patientFullName = effect.patientFullName,
                            photoUrl = effect.photoUrl,
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
        isLoading = false,
        patientData = null,
        listState = ParcelableListState.NotInitialized(),
    )

}

sealed class PatientDetailsEvent {
    // ui
    object RefreshRequest : PatientDetailsEvent()
    object LoadExamNextPage : PatientDetailsEvent()
    object PatientInfoTap : PatientDetailsEvent()

    object CreateExamTap : PatientDetailsEvent()

    object ReviewPatientTap : PatientDetailsEvent()

    object PhotoTakingChosen : PatientDetailsEvent()
    object ImagePickingChosen : PatientDetailsEvent()

    data class PoseInPhotoChecked(
        val uri: Uri,
    ) : PatientDetailsEvent()

    data class PhotoAccepted(
        val uri: Uri,
        val weight: Float,
        val height: Float,
    ) : PatientDetailsEvent()

    data class ExamTap(
        val id: Long
    ) : PatientDetailsEvent()

    object ExitTap : PatientDetailsEvent()

    object PhotoConfirmed : PatientDetailsEvent()

    data class PhotoRejected(
        val reason: String,
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
        val patientData: PatientLoadableDataModel
    ) : PatientDetailsEvent()

    object PhotoUploadSuccess : PatientDetailsEvent()
    data class PhotoUploadFailure(
        val message: String? = null,
    ) : PatientDetailsEvent()

    object CameraPermissionGranted : PatientDetailsEvent()
    object CameraPermissionDenied : PatientDetailsEvent()

    object WriteStoragePermissionGranted : PatientDetailsEvent()
    object WriteStoragePermissionDenied : PatientDetailsEvent()

    data class PhotoTaken(
        val uri: Uri
    ) : PatientDetailsEvent()

    data class ImagePicked(
        val uri: Uri
    ) : PatientDetailsEvent()

    object ConfirmPhotoSuccess : PatientDetailsEvent()
    object ConfirmPhotoFailure : PatientDetailsEvent()

    object RejectPhotoSuccess : PatientDetailsEvent()
    object RejectPhotoFailure : PatientDetailsEvent()


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

    object OpenImagePicker : PatientDetailsEffect()

    data class UploadPhoto(
        val patientId: Long,
        val uri: Uri,
        val weight: Float,
        val height: Float,
    ) : PatientDetailsEffect()

    data class CheckPoseInPhoto(
        val uri: Uri,
    ) : PatientDetailsEffect()

    data class AskToConfirmPhoto(
        val uri: Uri,
        val weight: Float,
        val height: Float,
    ) : PatientDetailsEffect()

    data class NavigateToPatientPhotoReview(
        val patientFullName: String,
        val photoUrl: String
    ) : PatientDetailsEffect()

    data class ConfirmPhoto(
        val id: Long,
    ) : PatientDetailsEffect()

    data class RejectPhoto(
        val id: Long,
        val reason: String,
    ) : PatientDetailsEffect()

    object ShowUnexpectedError : PatientDetailsEffect()
    data class ShowErrorMessage(
        val message: String,
    ) : PatientDetailsEffect()

    object AskHowToGetPhoto : PatientDetailsEffect()

    object CheckCameraPermission : PatientDetailsEffect()

    object CheckWriteStoragePermission : PatientDetailsEffect()

    object Exit : PatientDetailsEffect()
}

@Parcelize
data class PatientDetailsDataModel(
    val patientId: Long,
    val isLoading: Boolean,
    val patientData: PatientLoadableDataModel?,
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

@Parcelize
data class PatientLoadableDataModel(
    val fullName: String,
    val weight: Float,
    val height: Float,
    val photoToReview: PatientPhotoToReviewDataModel?
) : Parcelable

@Parcelize
data class PatientPhotoToReviewDataModel(
    val id: Long,
    val createdAt: LocalDateTime,
    val url: String,
) : Parcelable

data class PatientDetailsViewModel(
    val title: String?,
    val isLoaderVisible: Boolean,
    val isTakePhotoButtonEnabled: Boolean,
    val listState: ListState<ExamViewModel>,
    val photoToReview: PatientPhotoToReviewViewModel?,
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

data class PatientPhotoToReviewViewModel(
    val date: String,
)

fun PatientDetailsDataModel.viewModel(
    resourceProvider: ResourceProvider,
): PatientDetailsViewModel {
    val dateTimeFormatter = DateTimeFormatter
        .ofPattern("dd MMM yyyy, HH:mm")
        .withLocale(resourceProvider.getCurrentLocale())
    return PatientDetailsViewModel(
        title = patientData?.fullName,
        isLoaderVisible = isLoading,
        isTakePhotoButtonEnabled = isLoading.not(),
        listState = listState.plainState.map { exam, index, list ->
            exam.viewModel(
                resourceProvider = resourceProvider,
                dateTimeFormatter = dateTimeFormatter,
                isFirst = index == 0,
                isLast = index == list.size - 1
            )
        },
        photoToReview = patientData?.photoToReview?.let {
            PatientPhotoToReviewViewModel(
                date = TimeAgoFormatter(resourceProvider).format(it.createdAt)
            )
        }
    )
}

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