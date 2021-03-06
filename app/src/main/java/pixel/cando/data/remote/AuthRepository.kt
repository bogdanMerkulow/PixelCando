package pixel.cando.data.remote

import com.squareup.moshi.Moshi
import pixel.cando.data.models.PasswordRecoveryFailure
import pixel.cando.data.models.SignInFailure
import pixel.cando.data.models.SignInSuccess
import pixel.cando.data.models.UserRole
import pixel.cando.data.remote.dto.PasswordRecoveryRequest
import pixel.cando.data.remote.dto.SignInRequest
import pixel.cando.utils.Either
import pixel.cando.utils.handleSkippingCancellation

interface AuthRepository {

    suspend fun signIn(
        email: String,
        password: String,
    ): Either<SignInSuccess, SignInFailure>

    suspend fun recoverPassword(
        email: String
    ): Either<Unit, PasswordRecoveryFailure>

}

class RealAuthRepository(
    private val authApi: AuthApi,
    private val moshi: Moshi,
) : AuthRepository {

    override suspend fun signIn(
        email: String,
        password: String
    ): Either<SignInSuccess, SignInFailure> {
        return try {
            val response = authApi.signIn(
                SignInRequest(
                    email = email,
                    password = password
                )
            )
            if (response.isSuccessful) {
                val signInResponse = response.body()!!
                val userWrapperDto = signInResponse.doctor ?: signInResponse.patient
                userWrapperDto?.user?.let { userDto ->
                    userDto.role.userRole?.let { userRole ->
                        Either.Left(
                            SignInSuccess(
                                accessToken = userDto.accessToken,
                                userId = userDto.id,
                                userRole = userRole
                            )
                        )
                    }
                } ?: Either.Right(
                    SignInFailure.UnsupportedUserRole
                )
            } else {
                Either.Right(
                    response.errorMessage(moshi)?.let {
                        SignInFailure.CustomMessage(it)
                    } ?: SignInFailure.UnknownError(
                        IllegalStateException("Can not handle")
                    )
                )
            }
        } catch (t: Throwable) {
            t.handleSkippingCancellation {
                Either.Right(
                    SignInFailure.UnknownError(t)
                )
            }
        }
    }

    override suspend fun recoverPassword(
        email: String
    ): Either<Unit, PasswordRecoveryFailure> {
        return try {
            val response = authApi.recoverPassword(
                PasswordRecoveryRequest(
                    email = email
                )
            )
            if (response.isSuccessful) {
                Either.Left(
                    response.body()!!
                )
            } else {
                Either.Right(
                    response.errorMessage(moshi)?.let {
                        PasswordRecoveryFailure.CustomMessage(it)
                    } ?: PasswordRecoveryFailure.UnknownError(
                        IllegalStateException("Can not handle")
                    )
                )
            }
        } catch (t: Throwable) {
            t.handleSkippingCancellation {
                Either.Right(
                    PasswordRecoveryFailure.UnknownError(t)
                )
            }
        }
    }

}

private const val doctorRoleServerValue = "doctor"
private const val patientRoleServerValue = "patient"

private val String.userRole: UserRole?
    get() = when (this) {
        doctorRoleServerValue -> UserRole.DOCTOR
        patientRoleServerValue -> UserRole.PATIENT
        else -> null
    }