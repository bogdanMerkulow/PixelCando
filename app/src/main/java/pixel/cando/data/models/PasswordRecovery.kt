package pixel.cando.data.models

sealed class PasswordRecoveryFailure {
    data class UnknownError(
        val throwable: Throwable
    ) : PasswordRecoveryFailure()

    data class CustomMessage(
        val message: String
    ) : PasswordRecoveryFailure()
}