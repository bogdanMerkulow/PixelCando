package pixel.cando.ui.main.photo_list

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.Photo
import pixel.cando.data.models.PhotoState
import pixel.cando.data.models.UploadPhotoFailure
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.di.PhotoPreviewArguments
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui._common.NoDataListPlaceholder
import pixel.cando.utils.ImagePicker
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.PermissionChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.TimeAgoFormatter
import pixel.cando.utils.base64
import pixel.cando.utils.handleSkippingCancellation
import pixel.cando.utils.loadReducedBitmap
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight
import java.time.LocalDateTime

object PhotoListLogic {

    fun init(
        model: PhotoListDataModel
    ): First<PhotoListDataModel, PhotoListEffect> {
        return if (model.photos == null) {
            First.first(
                model.copy(
                    loadingState = PhotoListLoadingState.LOADING,
                ),
                setOf(
                    PhotoListEffect.LoadPhotos,
                    PhotoListEffect.LoadPatientData,
                )
            )
        } else First.first(model)
    }

    fun update(
        model: PhotoListDataModel,
        event: PhotoListEvent
    ): Next<PhotoListDataModel, PhotoListEffect> {
        return when (event) {
            // ui
            is PhotoListEvent.RefreshRequest -> {
                if (model.loadingState == PhotoListLoadingState.NONE) {
                    Next.next(
                        model.copy(
                            loadingState = PhotoListLoadingState.REFRESHING,
                        ),
                        setOf(
                            PhotoListEffect.LoadPhotos,
                            PhotoListEffect.LoadPatientData,
                        )
                    )
                } else Next.noChange()
            }
            is PhotoListEvent.DeletePhotoTap -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.AskToConfirmPhotoRemovalAndDeleteIfAgree(
                            photoId = event.photoId
                        )
                    )
                )
            }
            is PhotoListEvent.PhotoDeletionConfirmed -> {
                Next.next(
                    model.copy(
                        loadingState = PhotoListLoadingState.LOADING,
                    ),
                    setOf(
                        PhotoListEffect.DeletePhoto(event.photoId)
                    )
                )
            }
            is PhotoListEvent.AddPhotoClick -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.AskHowToGetPhoto
                    )
                )
            }
            is PhotoListEvent.PhotoTakingChosen -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.CheckCameraPermission
                    )
                )
            }
            is PhotoListEvent.ImagePickingChosen -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.OpenImagePicker
                    )
                )
            }
            is PhotoListEvent.PhotoAccepted -> {
                Next.next(
                    model.copy(
                        loadingState = PhotoListLoadingState.LOADING,
                    ),
                    setOf(
                        PhotoListEffect.UploadPhoto(
                            uri = event.uri,
                            weight = event.weight,
                        )
                    )
                )
            }
            is PhotoListEvent.PhotoTaken -> {
                val patientData = model.patientData
                if (patientData != null) {
                    Next.dispatch(
                        setOf(
                            PhotoListEffect.AskToConfirmPhoto(
                                uri = event.uri,
                                weight = patientData.weight,
                                weightUnit = patientData.weightUnit,
                                height = patientData.height,
                            )
                        )
                    )
                } else Next.noChange()
            }
            is PhotoListEvent.ImagePicked -> {
                val patientData = model.patientData
                if (patientData != null) {
                    Next.dispatch(
                        setOf(
                            PhotoListEffect.AskToConfirmPhoto(
                                uri = event.uri,
                                weight = patientData.weight,
                                weightUnit = patientData.weightUnit,
                                height = patientData.height,
                            )
                        )
                    )
                } else Next.noChange()
            }
            // model
            is PhotoListEvent.LoadPhotoListSuccess -> {
                Next.next(
                    model.copy(
                        photos = event.photos,
                        loadingState = PhotoListLoadingState.NONE,
                    )
                )
            }
            is PhotoListEvent.LoadPhotoListFailure -> {
                Next.next(
                    model.copy(
                        loadingState = PhotoListLoadingState.NONE,
                    ),
                    setOf(
                        PhotoListEffect.ShowUnexpectedError
                    )
                )
            }
            is PhotoListEvent.PhotoUploadSuccess -> {
                Next.next(
                    model.copy(
                        loadingState = PhotoListLoadingState.NONE,
                    ),
                    setOf(
                        PhotoListEffect.LoadPhotos
                    )
                )
            }
            is PhotoListEvent.PhotoUploadFailure -> {
                Next.next(
                    model.copy(
                        loadingState = PhotoListLoadingState.NONE,
                    ),
                    setOf(
                        if (event.message.isNullOrBlank()) PhotoListEffect.ShowUnexpectedError
                        else PhotoListEffect.ShowErrorMessage(event.message)
                    )
                )
            }
            is PhotoListEvent.PhotoRemovalSuccess -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.LoadPhotos
                    )
                )
            }
            is PhotoListEvent.PhotoRemovalFailure -> {
                Next.next(
                    model.copy(
                        loadingState = PhotoListLoadingState.NONE,
                    ),
                    setOf(
                        PhotoListEffect.ShowUnexpectedError
                    )
                )
            }
            is PhotoListEvent.LoadPatientDataSuccess -> {
                Next.next(
                    model.copy(
                        patientData = event.patientData,
                    )
                )
            }
            is PhotoListEvent.CameraPermissionGranted -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.CheckWriteStoragePermission
                    )
                )
            }
            is PhotoListEvent.CameraPermissionDenied -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.ShowUnexpectedError
                    )
                )
            }
            is PhotoListEvent.WriteStoragePermissionGranted -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.OpenPhotoTaker
                    )
                )
            }
            is PhotoListEvent.WriteStoragePermissionDenied -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.ShowUnexpectedError
                    )
                )
            }
        }
    }

    fun effectHandler(
        photoTakerOpener: () -> Unit,
        photoConfirmationAsker: (PhotoPreviewArguments) -> Unit,
        howToGetPhotoAsker: () -> Unit,
        photoRemovalConfirmationAsker: (Long) -> Unit,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        cameraPermissionChecker: PermissionChecker,
        writeStoragePermissionChecker: PermissionChecker,
        imagePicker: ImagePicker,
        context: Context,
    ): Connectable<PhotoListEffect, PhotoListEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PhotoListEffect.LoadPhotos -> {
                    val result = remoteRepository.getPatientPhotos()
                    result.onLeft {
                        output.accept(
                            PhotoListEvent.LoadPhotoListSuccess(
                                it.map {
                                    it.dataModel()
                                }
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            PhotoListEvent.LoadPhotoListFailure
                        )
                    }
                }
                is PhotoListEffect.LoadPatientData -> {
                    val result = remoteRepository.getPatientAccount()
                    val doctor = remoteRepository.getDoctor()
                    doctor.onLeft { doctor ->
                        result.onLeft {
                            output.accept(
                                PhotoListEvent.LoadPatientDataSuccess(
                                    PatientLoadableDataModel(
                                        weight = it.weight,
                                        weightUnit = doctor.units?.weight ?: "",
                                        height = it.height,
                                    )
                                )
                            )
                        }
                    }
                }
                is PhotoListEffect.UploadPhoto -> {
                    try {
                        val bitmap = context.loadReducedBitmap(effect.uri)
                        val base64 = bitmap.base64
                        bitmap.recycle()
                        val result = remoteRepository.uploadPhotoByPatient(
                            weight = effect.weight,
                            photo = base64
                        )
                        result.onLeft {
                            output.accept(
                                PhotoListEvent.PhotoUploadSuccess
                            )
                        }
                        result.onRight {
                            when (it) {
                                is UploadPhotoFailure.ErrorMessage -> {
                                    output.accept(
                                        PhotoListEvent.PhotoUploadFailure(
                                            message = it.message
                                        )
                                    )
                                }
                                is UploadPhotoFailure.UnknownError -> {
                                    logError(it.throwable)
                                    output.accept(
                                        PhotoListEvent.PhotoUploadFailure()
                                    )
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        t.handleSkippingCancellation {
                            logError(t)
                            output.accept(
                                PhotoListEvent.PhotoUploadFailure()
                            )
                        }
                    }
                }
                is PhotoListEffect.DeletePhoto -> {
                    val result = remoteRepository.deletePhoto(effect.photoId)
                    result.onLeft {
                        output.accept(
                            PhotoListEvent.PhotoRemovalSuccess
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            PhotoListEvent.PhotoRemovalFailure
                        )
                    }
                }
                is PhotoListEffect.OpenPhotoTaker -> {
                    photoTakerOpener.invoke()
                }
                is PhotoListEffect.OpenImagePicker -> {
                    imagePicker.pickImage()
                }
                is PhotoListEffect.CheckCameraPermission -> {
                    if (cameraPermissionChecker.checkPermission()) {
                        output.accept(
                            PhotoListEvent.CameraPermissionGranted
                        )
                    } else {
                        cameraPermissionChecker.requestPermission()
                    }
                }
                is PhotoListEffect.CheckWriteStoragePermission -> {
                    if (writeStoragePermissionChecker.checkPermission()) {
                        output.accept(
                            PhotoListEvent.WriteStoragePermissionGranted
                        )
                    } else {
                        writeStoragePermissionChecker.requestPermission()
                    }
                }
                is PhotoListEffect.AskHowToGetPhoto -> {
                    howToGetPhotoAsker.invoke()
                }
                is PhotoListEffect.AskToConfirmPhoto -> {
                    photoConfirmationAsker.invoke(
                        PhotoPreviewArguments(
                            uri = effect.uri,
                            weight = effect.weight,
                            weightUnit = effect.weightUnit,
                            height = effect.height,
                        )
                    )
                }
                is PhotoListEffect.AskToConfirmPhotoRemovalAndDeleteIfAgree -> {
                    photoRemovalConfirmationAsker.invoke(effect.photoId)
                }
                is PhotoListEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
                is PhotoListEffect.ShowErrorMessage -> {
                    messageDisplayer.showMessage(
                        effect.message
                    )
                }
            }
        }

    fun initialModel(
    ) = PhotoListDataModel(
        photos = null,
        patientData = null,
        loadingState = PhotoListLoadingState.NONE,
    )

}

sealed class PhotoListEvent {
    // ui
    object RefreshRequest : PhotoListEvent()

    object AddPhotoClick : PhotoListEvent()

    data class DeletePhotoTap(
        val photoId: Long
    ) : PhotoListEvent()

    data class PhotoDeletionConfirmed(
        val photoId: Long
    ) : PhotoListEvent()

    object PhotoTakingChosen : PhotoListEvent()
    object ImagePickingChosen : PhotoListEvent()

    data class PhotoAccepted(
        val uri: Uri,
        val weight: Float,
    ) : PhotoListEvent()

    data class PhotoTaken(
        val uri: Uri
    ) : PhotoListEvent()

    data class ImagePicked(
        val uri: Uri
    ) : PhotoListEvent()

    // model
    data class LoadPhotoListSuccess(
        val photos: List<PhotoDataModel>
    ) : PhotoListEvent()

    object LoadPhotoListFailure : PhotoListEvent()

    object PhotoUploadSuccess : PhotoListEvent()
    data class PhotoUploadFailure(
        val message: String? = null,
    ) : PhotoListEvent()

    object PhotoRemovalSuccess : PhotoListEvent()
    object PhotoRemovalFailure : PhotoListEvent()

    object CameraPermissionGranted : PhotoListEvent()
    object CameraPermissionDenied : PhotoListEvent()

    object WriteStoragePermissionGranted : PhotoListEvent()
    object WriteStoragePermissionDenied : PhotoListEvent()

    data class LoadPatientDataSuccess(
        val patientData: PatientLoadableDataModel
    ) : PhotoListEvent()

}

sealed class PhotoListEffect {

    object LoadPhotos : PhotoListEffect()

    object LoadPatientData : PhotoListEffect()

    data class AskToConfirmPhotoRemovalAndDeleteIfAgree(
        val photoId: Long
    ) : PhotoListEffect()

    data class DeletePhoto(
        val photoId: Long
    ) : PhotoListEffect()

    object OpenPhotoTaker : PhotoListEffect()

    object OpenImagePicker : PhotoListEffect()

    data class UploadPhoto(
        val uri: Uri,
        val weight: Float,
    ) : PhotoListEffect()

    data class AskToConfirmPhoto(
        val uri: Uri,
        val weight: Float,
        val weightUnit: String,
        val height: String,
    ) : PhotoListEffect()

    object ShowUnexpectedError : PhotoListEffect()
    data class ShowErrorMessage(
        val message: String,
    ) : PhotoListEffect()

    object AskHowToGetPhoto : PhotoListEffect()

    object CheckCameraPermission : PhotoListEffect()

    object CheckWriteStoragePermission : PhotoListEffect()
}

@Parcelize
data class PhotoListDataModel(
    val photos: List<PhotoDataModel>?,
    val patientData: PatientLoadableDataModel?,
    val loadingState: PhotoListLoadingState,
) : Parcelable

enum class PhotoListLoadingState {
    NONE, LOADING, REFRESHING
}

@Parcelize
data class PhotoDataModel(
    val id: Long,
    val imageUrl: String,
    val createdAt: LocalDateTime,
    val state: PhotoState,
    val note: String?,
) : Parcelable

@Parcelize
data class PatientLoadableDataModel(
    val weight: Float,
    val weightUnit: String,
    val height: String,
) : Parcelable

data class PhotoListViewModel(
    val listItems: List<PhotoListItem>,
    val isLoaderVisible: Boolean,
    val isRefreshing: Boolean,
    val isTakePhotoButtonEnabled: Boolean,
)

data class PhotoViewModel(
    val id: Long,
    val state: PhotoState,
    val imageUrl: String,
    val note: String,
    val date: String,
    val mayDelete: Boolean,
    val isFirst: Boolean,
    val isLast: Boolean,
)

sealed class PhotoListItem : ListItem {
    data class NoData(
        override val title: String,
        override val description: String,
    ) : PhotoListItem(),
        NoDataListPlaceholder

    data class Photo(
        val photo: PhotoViewModel
    ) : PhotoListItem()
}

fun PhotoListDataModel.viewModel(
    resourceProvider: ResourceProvider,
): PhotoListViewModel {
    val timeAgoFormatter = TimeAgoFormatter(resourceProvider)
    return PhotoListViewModel(
        listItems = when {
            loadingState == PhotoListLoadingState.LOADING -> emptyList()
            photos.isNullOrEmpty() -> listOf(
                PhotoListItem.NoData(
                    title = resourceProvider.getString(R.string.photo_list_no_photos_title),
                    description = resourceProvider.getString(R.string.photo_list_no_photos_description),
                )
            )
            else -> photos.mapIndexed { index, photo ->
                PhotoListItem.Photo(
                    photo.viewModel(
                        resourceProvider = resourceProvider,
                        timeAgoFormatter = timeAgoFormatter,
                        isFirst = index == 0,
                        isLast = photos.lastIndex == index
                    )
                )
            }
        },
        isLoaderVisible = loadingState == PhotoListLoadingState.LOADING,
        isRefreshing = loadingState == PhotoListLoadingState.REFRESHING,
        isTakePhotoButtonEnabled = loadingState == PhotoListLoadingState.NONE
                && photos?.any { it.state == PhotoState.PENDING }?.not() ?: false,
    )
}

private fun PhotoDataModel.viewModel(
    resourceProvider: ResourceProvider,
    timeAgoFormatter: TimeAgoFormatter,
    isFirst: Boolean,
    isLast: Boolean,
) = PhotoViewModel(
    id = id,
    state = state,
    imageUrl = imageUrl,
    note = when (state) {
        PhotoState.ACCEPTED -> resourceProvider.getString(R.string.photo_list_photo_was_accepted)
        PhotoState.PENDING -> resourceProvider.getString(R.string.photo_list_photo_wait_for_review)
        PhotoState.REJECTED -> note.orEmpty()
    },
    date = timeAgoFormatter.format(createdAt),
    mayDelete = state == PhotoState.PENDING,
    isFirst = isFirst,
    isLast = isLast,
)

private fun Photo.dataModel(
) = PhotoDataModel(
    id = id,
    imageUrl = imageUrl,
    createdAt = createdAt,
    state = state,
    note = note,
)