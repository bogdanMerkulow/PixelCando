package pixel.cando.ui.main.photo_list

import android.graphics.Bitmap
import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.PermissionChecker
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.base64ForSending

object PhotoListLogic {

    fun init(
        model: PhotoListDataModel
    ): First<PhotoListDataModel, PhotoListEffect> {
        return First.first(model)
    }

    fun update(
        model: PhotoListDataModel,
        event: PhotoListEvent
    ): Next<PhotoListDataModel, PhotoListEffect> {
        return when (event) {
            // ui
            is PhotoListEvent.UploadPhotoClick -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.CheckCameraPermission
                    )
                )
            }
            is PhotoListEvent.PhotoTaken -> {
                Next.next(
                    model.copy(
                        isLoading = true,
                    ),
                    setOf(
                        PhotoListEffect.UploadPhoto(
                            bitmap = event.bitmap
                        )
                    )
                )
            }
            // model
            is PhotoListEvent.PhotoUploadSuccess -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    )
                )
            }
            is PhotoListEvent.PhotoUploadFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        PhotoListEffect.ShowUnexpectedError
                    )
                )
            }
            is PhotoListEvent.CameraPermissionGranted -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.OpenPhotoTaker
                    )
                )
            }
            is PhotoListEvent.CameraPermissionDenied -> {
                Next.dispatch(
                    setOf(
                        PhotoListEffect.ShowUnexpectedError // TODO change the message
                    )
                )
            }
        }
    }

    fun effectHandler(
        photoTakerOpener: () -> Unit,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        cameraPermissionChecker: PermissionChecker,
    ): Connectable<PhotoListEffect, PhotoListEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PhotoListEffect.OpenPhotoTaker -> {
                    photoTakerOpener.invoke()
                }
                is PhotoListEffect.UploadPhoto -> {
                    val base64 = effect.bitmap.base64ForSending
                    if (base64 != null) {
//                        val result = remoteRepository.uploadPhoto(
//                            patientId = 666, // FIXME
//                            photo = base64
//                        )
//                        result.onLeft {
//                            output.accept(
//                                PhotoListEvent.PhotoUploadSuccess
//                            )
//                        }
//                        result.onRight {
//                            //logError(it)
//                            output.accept(
//                                PhotoListEvent.PhotoUploadFailure
//                            )
//                        }
                    } else {
                        output.accept(
                            PhotoListEvent.PhotoUploadFailure
                        )
                    }
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
                is PhotoListEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
            }
        }

    fun initialModel(
    ) = PhotoListDataModel(
        photos = emptyList(),
        isLoading = false,
    )

}

sealed class PhotoListEvent {
    // ui
    object UploadPhotoClick : PhotoListEvent()
    data class PhotoTaken(
        val bitmap: Bitmap
    ) : PhotoListEvent()

    // model
    object PhotoUploadSuccess : PhotoListEvent()
    object PhotoUploadFailure : PhotoListEvent()

    object CameraPermissionGranted : PhotoListEvent()
    object CameraPermissionDenied : PhotoListEvent()
}

sealed class PhotoListEffect {
    object OpenPhotoTaker : PhotoListEffect()
    data class UploadPhoto(
        val bitmap: Bitmap
    ) : PhotoListEffect()

    object ShowUnexpectedError : PhotoListEffect()

    object CheckCameraPermission : PhotoListEffect()
}

@Parcelize
data class PhotoListDataModel(
    val photos: List<Unit>,
    val isLoading: Boolean,
) : Parcelable

data class PhotoListViewModel(
    val listItems: List<PhotoListItem>,
    val isLoaderVisible: Boolean,
)

sealed class PhotoListItem : ListItem {

    object NoData : PhotoListItem()

}

fun PhotoListDataModel.viewModel(
) = PhotoListViewModel(
    listItems = when {
        isLoading -> emptyList()
        photos.isEmpty() -> listOf(PhotoListItem.NoData)
        else -> emptyList() // TODO replace with mapping of photos
    },
    isLoaderVisible = isLoading,
)