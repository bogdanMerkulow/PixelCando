package pixel.cando.data.remote

import pixel.cando.data.remote.dto.PasswordRecoveryRequest
import pixel.cando.data.remote.dto.SignInRequest
import pixel.cando.data.remote.dto.SignInResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("doctor/account/sign-in")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>

    @POST("account/password/forgot")
    suspend fun recoverPassword(
        @Body request: PasswordRecoveryRequest
    ): Response<Unit>

}