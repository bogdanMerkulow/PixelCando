package pixel.cando.data.remote

import pixel.cando.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RestApi {

    @POST("doctor/patients/list")
    suspend fun getPatients(
        @Body request: PatientListRequest
    ): Response<PatientListResponse>

    @POST("doctor/folders/list")
    suspend fun getFolders(
        @Body request: FolderListRequest
    ): Response<FolderListResponse>

    @POST("doctor/photos/upload")
    suspend fun uploadPhoto(
        @Body request: UploadPhotoForPatientRequest
    ): Response<Unit>

}