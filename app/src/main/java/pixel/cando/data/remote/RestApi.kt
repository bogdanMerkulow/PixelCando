package pixel.cando.data.remote

import pixel.cando.data.remote.dto.ChatListRequest
import pixel.cando.data.remote.dto.ChatListResponse
import pixel.cando.data.remote.dto.ChatMessageListRequest
import pixel.cando.data.remote.dto.ChatMessageListResponse
import pixel.cando.data.remote.dto.ConfirmPhotoRequest
import pixel.cando.data.remote.dto.DeviceRegisterRequest
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
import pixel.cando.data.remote.dto.RejectPhotoRequest
import pixel.cando.data.remote.dto.SendChatMessageRequest
import pixel.cando.data.remote.dto.UpdateAccountRequest
import pixel.cando.data.remote.dto.UpdateAccountResponse
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

    @POST("doctor/account/update")
    suspend fun updateAccount(
        @Body request: UpdateAccountRequest
    ): Response<UpdateAccountResponse>

    @POST("account/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegisterRequest
    ): Response<Unit>

    @POST("doctor/photos/confirm")
    suspend fun confirmPhoto(
        @Body request: ConfirmPhotoRequest
    ): Response<Unit>

    @POST("doctor/photos/reject")
    suspend fun rejectPhoto(
        @Body request: RejectPhotoRequest
    ): Response<Unit>

    @POST("doctor/chat/participants")
    suspend fun getChats(
        @Body request: ChatListRequest
    ): Response<ChatListResponse>

    @POST("doctor/chat/feed")
    suspend fun getChatMessages(
        @Body request: ChatMessageListRequest
    ): Response<ChatMessageListResponse>

    @POST("doctor/chat/send")
    suspend fun sendChatMessage(
        @Body request: SendChatMessageRequest
    ): Response<Unit>

}