package pixel.cando.data.models

data class SignInSuccess(
    val accessToken: String,
    val userId: Long,
    val userRole: UserRole,
)

sealed class SignInFailure {
    object UnsupportedUserRole : SignInFailure()
    data class UnknownError(
        val throwable: Throwable
    ) : SignInFailure()

    data class CustomMessage(
        val message: String
    ) : SignInFailure()
}