package pixel.cando.data.remote

import pixel.cando.data.remote.dto.SignInRequest
import pixel.cando.data.remote.dto.SignInResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("account/auth/sign-in")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>


}