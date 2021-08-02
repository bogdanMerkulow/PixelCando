package pixel.cando.data.remote

import pixel.cando.data.remote.dto.PatientListRequest
import pixel.cando.data.remote.dto.PatientListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RestApi {

    @POST("customer/patients/list")
    suspend fun getPatients(
        @Body request: PatientListRequest
    ): Response<PatientListResponse>

}