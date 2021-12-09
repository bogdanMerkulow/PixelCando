package pixel.cando.data.remote

import pixel.cando.data.remote.dto.ChatListRequest
import pixel.cando.data.remote.dto.ChatListResponse
import pixel.cando.data.remote.dto.ChatMessageListResponse
import pixel.cando.data.remote.dto.ChatWithDoctorMessageListRequest
import pixel.cando.data.remote.dto.ChatWithPatientMessageListRequest
import pixel.cando.data.remote.dto.ConfirmPhotoRequest
import pixel.cando.data.remote.dto.DeviceRegisterRequest
import pixel.cando.data.remote.dto.EmptyRequest
import pixel.cando.data.remote.dto.ExamListRequest
import pixel.cando.data.remote.dto.ExamListResponse
import pixel.cando.data.remote.dto.FolderListRequest
import pixel.cando.data.remote.dto.FolderListResponse
import pixel.cando.data.remote.dto.GetDoctorAccountResponse
import pixel.cando.data.remote.dto.GetExamRequest
import pixel.cando.data.remote.dto.GetExamResponse
import pixel.cando.data.remote.dto.GetPatientAccountResponse
import pixel.cando.data.remote.dto.PatientGetRequest
import pixel.cando.data.remote.dto.PatientGetResponse
import pixel.cando.data.remote.dto.PatientListRequest
import pixel.cando.data.remote.dto.PatientListResponse
import pixel.cando.data.remote.dto.PhotoListResponse
import pixel.cando.data.remote.dto.ReadChatWithDoctorMessagesRequest
import pixel.cando.data.remote.dto.ReadChatWithPatientMessagesRequest
import pixel.cando.data.remote.dto.RejectPhotoRequest
import pixel.cando.data.remote.dto.SendMessageToChatWithDoctorRequest
import pixel.cando.data.remote.dto.SendMessageToChatWithPatientRequest
import pixel.cando.data.remote.dto.UpdateDoctorAccountRequest
import pixel.cando.data.remote.dto.UpdateDoctorAccountResponse
import pixel.cando.data.remote.dto.UpdatePatientAccountRequest
import pixel.cando.data.remote.dto.UpdatePatientAccountResponse
import pixel.cando.data.remote.dto.UploadPhotoByDoctorRequest
import pixel.cando.data.remote.dto.UploadPhotoByPatientRequest
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
    suspend fun uploadPhotoByDoctor(
        @Body request: UploadPhotoByDoctorRequest
    ): Response<Unit>

    @POST("patient/photos/upload")
    suspend fun uploadPhotoByPatient(
        @Body request: UploadPhotoByPatientRequest
    ): Response<Unit>

    @POST("doctor/account/get")
    suspend fun getDoctorAccount(
        @Body request: EmptyRequest
    ): Response<GetDoctorAccountResponse>

    @POST("doctor/account/update")
    suspend fun updateDoctorAccount(
        @Body request: UpdateDoctorAccountRequest
    ): Response<UpdateDoctorAccountResponse>

    @POST("patient/account/get")
    suspend fun getPatientAccount(
        @Body request: EmptyRequest
    ): Response<GetPatientAccountResponse>

    @POST("patient/account/update")
    suspend fun updatePatientAccount(
        @Body request: UpdatePatientAccountRequest
    ): Response<UpdatePatientAccountResponse>

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
    suspend fun getChatWithPatientMessages(
        @Body request: ChatWithPatientMessageListRequest
    ): Response<ChatMessageListResponse>

    @POST("patient/chat/feed")
    suspend fun getChatWithDoctorMessages(
        @Body request: ChatWithDoctorMessageListRequest
    ): Response<ChatMessageListResponse>

    @POST("doctor/chat/send")
    suspend fun sendMessageToChatWithPatient(
        @Body request: SendMessageToChatWithPatientRequest
    ): Response<Unit>

    @POST("patient/chat/send")
    suspend fun sendMessageToChatWithDoctor(
        @Body request: SendMessageToChatWithDoctorRequest
    ): Response<Unit>

    @POST("doctor/chat/read")
    suspend fun readMessagesInChatWithPatient(
        @Body request: ReadChatWithPatientMessagesRequest
    ): Response<Unit>

    @POST("patient/chat/read")
    suspend fun readMessagesInChatWithDoctor(
        @Body request: ReadChatWithDoctorMessagesRequest
    ): Response<Unit>

    @POST("patient/photos/list")
    suspend fun getPatientPhotos(
        @Body request: EmptyRequest
    ): Response<PhotoListResponse>

}