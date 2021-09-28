package pixel.cando.data.models

sealed class UploadPhotoFailure {
    data class ErrorMessage(
        val message: String,
    ) : UploadPhotoFailure()

    data class UnknownError(
        val throwable: Throwable
    ) : UploadPhotoFailure()
}