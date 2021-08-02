package pixel.cando.data.remote

import com.squareup.moshi.Moshi
import pixel.cando.data.models.SignInFailure
import pixel.cando.data.models.SignInSuccess
import pixel.cando.data.models.UserRole
import pixel.cando.data.remote.dto.FailureResponse
import pixel.cando.data.remote.dto.PasswordRecoveryRequest
import pixel.cando.data.remote.dto.SignInRequest
import pixel.cando.utils.Either
import pixel.cando.utils.objectFromJson
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
                response.body()!!.customer.user.let { user ->
                    user.role.userRole?.let { userRole ->
                        Either.Left(
                            SignInSuccess(
                                accessToken = user.accessToken,
                                userRole = userRole
                            )
                        )
                    } ?: Either.Right(
                        SignInFailure.UnsupportedUserRole
                    )
                }
            } else {
                Either.Right(
                    response.errorMessage?.let {
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

    private val <R> Response<R>.errorMessage: String?
        get() {
            if (isSuccessful.not()) {
                val failureResponse: FailureResponse? = moshi.objectFromJson(
                    errorBody()!!.string()
                )
                return failureResponse?.exception?.message
            }
            return null
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