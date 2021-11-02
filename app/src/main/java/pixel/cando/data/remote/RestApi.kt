package pixel.cando.data.remote

import pixel.cando.data.remote.dto.EmptyRequest
import pixel.cando.data.remote.dto.ExamListRequest
import pixel.cando.data.remote.dto.ExamListResponse
import pixel.cando.data.remote.dto.FolderListRequest
import pixel.cando.data.remote.dto.FolderListResponse
import pixel.cando.data.remote.dto.GetAccountResponse
import pixel.cando.data.remote.dto.GetExamRequest
import pixel.cando.data.remote.dto.GetExamResponse
import pixel.cando.data.remote.dto.PatientGetRequest
import pixel.cando.data.remote.dto.PatientGetResponse
import pixel.cando.data.remote.dto.PatientListRequest
import pixel.cando.data.remote.dto.PatientListResponse
import pixel.cando.data.remote.dto.UploadPhotoForPatientRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RestApi {

    @POST("doctor/patients/list")
    suspend fun getPatients(
        @Body request: PatientListRequest
    ): Response<PatientListResponse>

    @POST("doctor/patients/get")
    suspend fun getPatient(
        @Body request: PatientGetRequest
    ): Response<PatientGetResponse>

    @POST("doctor/folders/list")
    suspend fun getFolders(
        @Body request: FolderListRequest
    ): Response<FolderListResponse>

    @POST("doctor/exams/list")
    suspend fun getExams(
        @Body request: ExamListRequest
    ): Response<ExamListResponse>

    @POST("doctor/exams/get")
    suspend fun getExam(
        @Body request: GetExamRequest
    ): Response<GetExamResponse>

    @POST("doctor/photos/upload")
    suspend fun uploadPhoto(
        @Body request: UploadPhotoForPatientRequest
    ): Response<Unit>

    @POST("doctor/account/get")
    suspend fun getAccount(
        @Body request: EmptyRequest
    ): Response<GetAccountResponse>

}