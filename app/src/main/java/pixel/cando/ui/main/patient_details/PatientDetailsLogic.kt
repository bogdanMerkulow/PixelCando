package pixel.cando.ui.main.patient_details

import android.Manifest
import android.graphics.Bitmap
import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.*

object PatientDetailsLogic {

    fun init(
        model: PatientDetailsDataModel
    ): First<PatientDetailsDataModel, PatientDetailsEffect> {
        return First.first(model)
    }

    fun update(
        model: PatientDetailsDataModel,
        event: PatientDetailsEvent
    ): Next<PatientDetailsDataModel, PatientDetailsEffect> {
        return when (event) {
            // ui
            is PatientDetailsEvent.TakePhotoTap -> {
                Next.dispatch(
                    setOf(
                        PatientDetailsEffect.CheckCameraPermission
                    )
                )
            }
            is PatientDetailsEvent.PhotoTaken -> {
                Next.next(
                    model.copy(
                        isLoading = true,
                    ),
                    setOf(
                        PatientDetailsEffect.UploadPhoto(
                            bitmap = event.bitmap
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
            // model
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
                        PatientDetailsEffect.ShowUnexpectedError
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

    fun effectHandler(
        photoTakerOpener: () -> Unit,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        permissionChecker: PermissionChecker,
        flowRouter: FlowRouter,
    ): Connectable<PatientDetailsEffect, PatientDetailsEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PatientDetailsEffect.OpenPhotoTaker -> {
                    photoTakerOpener.invoke()
                }
                is PatientDetailsEffect.UploadPhoto -> {
                    val base64 = effect.bitmap.base64ForSending
                    if (base64 != null) {
                        val result = remoteRepository.uploadPhoto(
                            photo = base64
                        )
                        result.onLeft {
                            output.accept(
                                PatientDetailsEvent.PhotoUploadSuccess
                            )
                        }
                        result.onRight {
                            logError(it)
                            output.accept(
                                PatientDetailsEvent.PhotoUploadFailure
                            )
                        }
                    } else {
                        output.accept(
                            PatientDetailsEvent.PhotoUploadFailure
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
                is PatientDetailsEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
                is PatientDetailsEffect.Exit -> {
                    flowRouter.exit()
                }
            }
        }

    fun initialModel(
        patientId: Long,
    ) = PatientDetailsDataModel(
        patientId = patientId,
        isLoading = false,
    )

}

sealed class PatientDetailsEvent {
    // ui
    object TakePhotoTap : PatientDetailsEvent()
    data class PhotoTaken(
        val bitmap: Bitmap
    ) : PatientDetailsEvent()

    object ExitTap : PatientDetailsEvent()

    // model
    object PhotoUploadSuccess : PatientDetailsEvent()
    object PhotoUploadFailure : PatientDetailsEvent()

    object CameraPermissionGranted : PatientDetailsEvent()
    object CameraPermissionDenied : PatientDetailsEvent()
}

sealed class PatientDetailsEffect {
    object OpenPhotoTaker : PatientDetailsEffect()
    data class UploadPhoto(
        val bitmap: Bitmap
    ) : PatientDetailsEffect()

    object ShowUnexpectedError : PatientDetailsEffect()

    object CheckCameraPermission : PatientDetailsEffect()

    object Exit : PatientDetailsEffect()
}

@Parcelize
data class PatientDetailsDataModel(
    val patientId: Long,
    val isLoading: Boolean,
) : Parcelable

data class PatientDetailsViewModel(
    val isLoaderVisible: Boolean,
    val isTakePhotoButtonEnabled: Boolean,
)

fun PatientDetailsDataModel.viewModel(
) = PatientDetailsViewModel(
    isLoaderVisible = isLoading,
    isTakePhotoButtonEnabled = isLoading.not(),
)