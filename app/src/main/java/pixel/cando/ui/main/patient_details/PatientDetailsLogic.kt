package pixel.cando.ui.main.patient_details

import android.Manifest
import android.graphics.Bitmap
import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.UploadPhotoFailure
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.*

object PatientDetailsLogic {

    fun init(
        model: PatientDetailsDataModel
    ): First<PatientDetailsDataModel, PatientDetailsEffect> {
        return First.first(
            model,
            setOf(
                PatientDetailsEffect.LoadPatientInfo(
                    patientId = model.patientId,
                )
            )
        )
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

    fun effectHandler(
        photoTakerOpener: () -> Unit,
        photoConfirmationAsker: (Bitmap) -> Unit,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        permissionChecker: PermissionChecker,
        flowRouter: FlowRouter,
    ): Connectable<PatientDetailsEffect, PatientDetailsEvent> =
        CoroutineScopeEffectHandler { effect, output ->
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

    fun initialModel(
        patientId: Long,
    ) = PatientDetailsDataModel(
        patientId = patientId,
        patientFullName = null,
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

    data class PhotoAccepted(
        val bitmap: Bitmap
    ) : PatientDetailsEvent()

    object PhotoDeclined : PatientDetailsEvent()

    // model

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
) : Parcelable

data class PatientDetailsViewModel(
    val title: String,
    val isLoaderVisible: Boolean,
    val isTakePhotoButtonEnabled: Boolean,
)

fun PatientDetailsDataModel.viewModel(
) = PatientDetailsViewModel(
    title = patientFullName ?: "",
    isLoaderVisible = isLoading,
    isTakePhotoButtonEnabled = isLoading.not(),
)