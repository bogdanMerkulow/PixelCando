package pixel.cando.data.remote

import com.squareup.moshi.Moshi
import pixel.cando.data.models.SignInFailure
import pixel.cando.data.models.SignInSuccess
import pixel.cando.data.models.UserRole
import pixel.cando.data.remote.dto.PasswordRecoveryRequest
import pixel.cando.data.remote.dto.SignInRequest
import pixel.cando.utils.Either
import retrofit2.Response

interface AuthRepository {

    suspend fun signIn(
        email: String,
        password: String,
    ): Either<SignInSuccess, SignInFailure>

    suspend fun recoverPassword(
        email: String
    ): Either<Unit, Throwable>

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
                val userWrapperDto = signInResponse.customer ?: signInResponse.patient
                userWrapperDto?.user?.let { userDto ->
                    userDto.role.userRole?.let { userRole ->
                        Either.Left(
                            SignInSuccess(
                                accessToken = userDto.accessToken,
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
        } catch (ex: Throwable) {
            Either.Right(
                SignInFailure.UnknownError(ex)
            )
        }
    }

    override suspend fun recoverPassword(
        email: String
    ): Either<Unit, Throwable> {
        return callApi {
            authApi.recoverPassword(
                PasswordRecoveryRequest(
                    email = email
                )
            )
        }
    }

    private suspend fun <R> callApi(
        action: suspend () -> Response<R>
    ): Either<R, Throwable> {
        return try {
            val response = action.invoke()
            if (response.isSuccessful) {
                Either.Left(response.body()!!)
            } else {
                Either.Right(
                    IllegalStateException(
                        "Unsuccessful response received"
                    )
                )
            }
        } catch (ex: Throwable) {
            Either.Right(ex)
        }
    }

}

private const val doctorRoleServerValue = "customer"
private const val patientRoleServerValue = "patient"

private val String.userRole: UserRole?
    get() = when (this) {
        doctorRoleServerValue -> UserRole.DOCTOR
        patientRoleServerValue -> UserRole.PATIENT
        else -> null
    }

private val UserRole.serverValue: String
    get() = when (this) {
        UserRole.DOCTOR -> doctorRoleServerValue
        UserRole.PATIENT -> patientRoleServerValue
    }