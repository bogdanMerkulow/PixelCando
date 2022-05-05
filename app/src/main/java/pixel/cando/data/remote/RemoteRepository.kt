package pixel.cando.data.remote

import com.squareup.moshi.Moshi
import pixel.cando.data.models.ChatItem
import pixel.cando.data.models.ChatMessage
import pixel.cando.data.models.ChatRecentMessage
import pixel.cando.data.models.Doctor
import pixel.cando.data.models.DoctorAccount
import pixel.cando.data.models.ExamListItemInfo
import pixel.cando.data.models.ExamSingleItemInfo
import pixel.cando.data.models.Folder
import pixel.cando.data.models.Gender
import pixel.cando.data.models.MessageListPortion
import pixel.cando.data.models.PatientAccount
import pixel.cando.data.models.PatientAccountUpdated
import pixel.cando.data.models.PatientListItemInfo
import pixel.cando.data.models.PatientPhotoToReview
import pixel.cando.data.models.PatientSingleItemInfo
import pixel.cando.data.models.Photo
import pixel.cando.data.models.PhotoState
import pixel.cando.data.models.Units
import pixel.cando.data.models.UploadPhotoFailure
import pixel.cando.data.remote.dto.ChatItemDto
import pixel.cando.data.remote.dto.ChatListFilterDto
import pixel.cando.data.remote.dto.ChatListRequest
import pixel.cando.data.remote.dto.ChatMessageListFilterDto
import pixel.cando.data.remote.dto.ChatMessageListResponse
import pixel.cando.data.remote.dto.ChatWithDoctorMessageListRequest
import pixel.cando.data.remote.dto.ChatWithPatientMessageListRequest
import pixel.cando.data.remote.dto.ConfirmPhotoRequest
import pixel.cando.data.remote.dto.DeletePhotoRequest
import pixel.cando.data.remote.dto.DeviceRegisterDto
import pixel.cando.data.remote.dto.DeviceRegisterRequest
import pixel.cando.data.remote.dto.DoctorAccountUserDto
import pixel.cando.data.remote.dto.DoctorUpdateAccountDto
import pixel.cando.data.remote.dto.EmptyRequest
import pixel.cando.data.remote.dto.ExamListRequest
import pixel.cando.data.remote.dto.FolderListRequest
import pixel.cando.data.remote.dto.GetDoctorAccountResponse
import pixel.cando.data.remote.dto.GetExamRequest
import pixel.cando.data.remote.dto.PatientAccountDto
import pixel.cando.data.remote.dto.PatientAccountUpdateDto
import pixel.cando.data.remote.dto.PatientGetRequest
import pixel.cando.data.remote.dto.PatientListFilterDto
import pixel.cando.data.remote.dto.PatientListRequest
import pixel.cando.data.remote.dto.ReadChatWithDoctorMessagesRequest
import pixel.cando.data.remote.dto.ReadChatWithPatientMessagesRequest
import pixel.cando.data.remote.dto.RejectPhotoRequest
import pixel.cando.data.remote.dto.SendMessageToChatWithDoctorDto
import pixel.cando.data.remote.dto.SendMessageToChatWithDoctorRequest
import pixel.cando.data.remote.dto.SendMessageToChatWithPatientDto
import pixel.cando.data.remote.dto.SendMessageToChatWithPatientRequest
import pixel.cando.data.remote.dto.UnitsDto
import pixel.cando.data.remote.dto.UpdateDoctorAccountRequest
import pixel.cando.data.remote.dto.UpdatePatientAccountDto
import pixel.cando.data.remote.dto.UpdatePatientAccountRequest
import pixel.cando.data.remote.dto.UpdatePatientAccountUserDto
import pixel.cando.data.remote.dto.UploadPhotoByDoctorRequest
import pixel.cando.data.remote.dto.UploadPhotoByPatientRequest
import pixel.cando.data.remote.dto.UploadPhotoForPatientWeightHeightDto
import pixel.cando.utils.Either
import pixel.cando.utils.handleSkippingCancellation
import pixel.cando.utils.logError
import pixel.cando.utils.mapOnlyLeft
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime

interface RemoteRepository {

    suspend fun getPatients(
        folderId: Long?,
        searchQuery: String?,
        page: Int,
    ): Either<List<PatientListItemInfo>, Throwable>

    suspend fun getPatient(
        patientId: Long,
    ): Either<PatientSingleItemInfo, Throwable>

    suspend fun getFolders(
    ): Either<List<Folder>, Throwable>

    suspend fun getExams(
        patientId: Long,
        page: Int,
    ): Either<List<ExamListItemInfo>, Throwable>

    suspend fun getExam(
        id: Long
    ): Either<ExamSingleItemInfo, Throwable>

    suspend fun uploadPhotoByDoctor(
        patientId: Long,
        weight: Float,
        height: String,
        photo: String
    ): Either<Unit, UploadPhotoFailure>

    suspend fun uploadPhotoByPatient(
        weight: Float,
        photo: String
    ): Either<Unit, UploadPhotoFailure>

    suspend fun getDoctor(): Either<Doctor, Throwable>

    suspend fun getDoctorAccount(
    ): Either<DoctorAccount, Throwable>

    suspend fun updateDoctorAccount(
        account: DoctorAccount
    ): Either<DoctorAccount, Throwable>

    suspend fun getPatientAccount(
    ): Either<PatientAccount, Throwable>

    suspend fun updatePatientAccount(
        fullName: String,
        email: String,
        phoneNumber: String?,
        contactEmail: String?,
        address: String?,
        city: String?,
        postalCode: String?,
        measurement: String?
    ): Either<PatientAccountUpdated, Throwable>

    suspend fun subscribeForPushNotifications(
        identifier: String
    ): Either<Unit, Throwable>

    suspend fun confirmPhoto(
        id: Long,
    ): Either<Unit, Throwable>

    suspend fun rejectPhoto(
        id: Long,
        reason: String,
    ): Either<Unit, Throwable>

    suspend fun getChats(
        folderId: Long?,
        page: Int,
    ): Either<List<ChatItem>, Throwable>

    suspend fun getChatsForPages(
        folderId: Long?,
        pageCount: Int,
    ): Either<List<ChatItem>, Throwable>

    suspend fun getChatWithPatientMessages(
        userId: Long,
        offset: Int,
        count: Int,
        sinceDate: LocalDateTime?,
    ): Either<MessageListPortion, Throwable>

    suspend fun sendMessageToChatWithPatient(
        userId: Long,
        message: String,
    ): Either<Unit, Throwable>

    suspend fun readMessagesInChatWithPatient(
        userId: Long,
        until: LocalDateTime,
    ): Either<Unit, Throwable>

    suspend fun getChatWithDoctorMessages(
        offset: Int,
        count: Int,
        sinceDate: LocalDateTime?,
    ): Either<MessageListPortion, Throwable>

    suspend fun sendMessageToChatWithDoctor(
        message: String,
    ): Either<Unit, Throwable>

    suspend fun readMessagesInChatWithDoctor(
        until: LocalDateTime,
    ): Either<Unit, Throwable>

    suspend fun getPatientPhotos(
    ): Either<List<Photo>, Throwable>

    suspend fun deletePhoto(
        photoId: Long
    ): Either<Unit, Throwable>

}

class RealRemoteRepository(
    private val restApi: RestApi,
    private val moshi: Moshi,
    private val unauthorizedHandler: () -> Unit,
) : RemoteRepository {

    private val pageSize = 20

    override suspend fun getPatients(
        folderId: Long?,
        searchQuery: String?,
        page: Int,
    ): Either<List<PatientListItemInfo>, Throwable> {
        return callApi {
            getPatients(
                PatientListRequest(
                    offset = page * pageSize,
                    limit = pageSize,
                    filter = PatientListFilterDto(
                        query = searchQuery,
                        folderId = folderId,
                    )
                )
            )
        }.mapOnlyLeft {
            it.results.map {
                PatientListItemInfo(
                    id = it.userId,
                    fullName = it.user.fullName,
                    gender = it.gender.toGender(),
                    age = it.age,
                    avatarText = it.user.avatar.text,
                    avatarBgColor = it.user.avatar.color,
                    lastExamAt = it.lastExamAt,
                )
            }
        }
    }

    override suspend fun getPatient(
        patientId: Long
    ): Either<PatientSingleItemInfo, Throwable> {
        return callApi {
            getPatient(
                PatientGetRequest(
                    id = patientId
                )
            )
        }.mapOnlyLeft {
            PatientSingleItemInfo(
                id = it.patient.userId,
                fullName = it.patient.user.fullName,
                gender = it.patient.gender.toGender(),
                age = it.patient.age,
                weight = it.patient.weight,
                height = it.patient.height,
                phoneNumber = it.patient.user.contactPhone,
                email = it.patient.user.contactEmail,
                address = it.patient.user.address,
                country = it.patient.user.country,
                city = it.patient.user.city,
                postalCode = it.patient.user.postalCode,
                photoToReview = it.patient.photo?.let {
                    PatientPhotoToReview(
                        id = it.id,
                        createdAt = it.createdAt,
                        url = it.file.original
                    )
                }
            )
        }
    }

    override suspend fun getFolders(
    ): Either<List<Folder>, Throwable> {
        return callApi {
            getFolders(
                FolderListRequest(
                    offset = 0,
                    limit = -1,
                )
            )
        }.mapOnlyLeft {
            it.folders.map {
                Folder(
                    id = it.id,
                    title = it.title,
                )
            }
        }
    }

    override suspend fun getExams(
        patientId: Long,
        page: Int
    ): Either<List<ExamListItemInfo>, Throwable> {
        return callApi {
            getExams(
                ExamListRequest(
                    patientId = patientId,
                    offset = page * pageSize,
                    limit = pageSize,
                )
            )
        }.mapOnlyLeft {
            it.results.map {
                ExamListItemInfo(
                    id = it.id,
                    createdAt = it.createdAt,
                    number = it.no,
                    weight = it.params.weight,
                    fatMass = it.params.fm,
                    fatFreeMass = it.params.ffm,
                    abdominalFatMass = it.params.abdominalFm,
                    bmi = it.params.bmi,
                )
            }
        }
    }

    override suspend fun getExam(
        id: Long
    ): Either<ExamSingleItemInfo, Throwable> {
        return callApi {
            getExam(
                GetExamRequest(id)
            )
        }.mapOnlyLeft {
            it.exam.let {
                ExamSingleItemInfo(
                    id = it.id,
                    createdAt = it.createdAt,
                    number = it.no,
                    weight = it.params.weight,
                    bmi = it.params.bmi,
                    bmr = it.params.bmr,
                    fm = it.params.fm,
                    ffm = it.params.ffm,
                    abdominalFatMass = it.params.abdominalFm,
                    tbw = it.params.tbw,
                    hip = it.params.hip,
                    belly = it.params.belly,
                    waistToHeight = it.params.waistToHeight,
                    silhouetteUrl = it.silhouette,
                )
            }
        }
    }

    override suspend fun uploadPhotoByDoctor(
        patientId: Long,
        weight: Float,
        height: String,
        photo: String,
    ): Either<Unit, UploadPhotoFailure> {
        return callApi(
            action = {
                uploadPhotoByDoctor(
                    UploadPhotoByDoctorRequest(
                        patientId = patientId,
                        weightHeight = UploadPhotoForPatientWeightHeightDto(
                            weight = weight,
                            height = height,
                        ),
                        photo = photo,
                    )
                )
            },
            unsuccessfulResponseMapper = {
                val errorMessage = it.errorMessage(moshi)
                Either.Right(
                    if (errorMessage != null) UploadPhotoFailure.ErrorMessage(errorMessage)
                    else UploadPhotoFailure.UnknownError(
                        IllegalArgumentException()
                    )
                )
            },
            notAuthorizedHandler = {
                Either.Right(
                    UploadPhotoFailure.UnknownError(it)
                )
            },
            unknownErrorHandler = {
                Either.Right(
                    UploadPhotoFailure.UnknownError(it)
                )
            },
        )
    }

    override suspend fun uploadPhotoByPatient(
        weight: Float,
        photo: String
    ): Either<Unit, UploadPhotoFailure> {
        return callApi(
            action = {
                uploadPhotoByPatient(
                    UploadPhotoByPatientRequest(
                        weight = weight,
                        photo = photo,
                    )
                )
            },
            unsuccessfulResponseMapper = {
                val errorMessage = it.errorMessage(moshi)
                Either.Right(
                    if (errorMessage != null) UploadPhotoFailure.ErrorMessage(errorMessage)
                    else UploadPhotoFailure.UnknownError(
                        IllegalArgumentException()
                    )
                )
            },
            notAuthorizedHandler = {
                Either.Right(
                    UploadPhotoFailure.UnknownError(it)
                )
            },
            unknownErrorHandler = {
                Either.Right(
                    UploadPhotoFailure.UnknownError(it)
                )
            },
        )
    }

    override suspend fun getDoctor(): Either<Doctor, Throwable> {
        return callApi {
            getDoctorAccount(
                EmptyRequest()
            )
        }.mapOnlyLeft {
            it.model()
        }
    }

    override suspend fun getDoctorAccount(
    ): Either<DoctorAccount, Throwable> {
        return callApi {
            getDoctorAccount(
                EmptyRequest()
            )
        }.mapOnlyLeft {
            it.doctor.user.model()
        }
    }

    override suspend fun updateDoctorAccount(
        account: DoctorAccount
    ): Either<DoctorAccount, Throwable> {
        return callApi {
            updateDoctorAccount(
                UpdateDoctorAccountRequest(
                    DoctorUpdateAccountDto(
                        DoctorAccountUserDto(
                            fullName = account.fullName,
                            email = account.email,
                            contactPhone = account.phoneNumber,
                            contactEmail = account.contactEmail,
                            address = account.address,
                            country = account.country,
                            city = account.city,
                            postalCode = account.postalCode,
                            measurement = account.measurement
                        )
                    )
                )
            )
        }.mapOnlyLeft {
            it.doctor.user.model()
        }
    }

    override suspend fun getPatientAccount(
    ): Either<PatientAccount, Throwable> {
        return callApi {
            getPatientAccount(
                EmptyRequest()
            )
        }.mapOnlyLeft {
            it.patient.model()
        }
    }

    override suspend fun updatePatientAccount(
        fullName: String,
        email: String,
        phoneNumber: String?,
        contactEmail: String?,
        address: String?,
        city: String?,
        postalCode: String?,
        measurement: String?
    ): Either<PatientAccountUpdated, Throwable> {
        return callApi {
            updatePatientAccount(
                UpdatePatientAccountRequest(
                    UpdatePatientAccountDto(
                        user = UpdatePatientAccountUserDto(
                            fullName = fullName,
                            email = email,
                            contactPhone = phoneNumber,
                            contactEmail = contactEmail,
                            address = address,
                            city = city,
                            postalCode = postalCode,
                            measurement = measurement
                        ),
                    )
                )
            )
        }.mapOnlyLeft {
            it.patient.model()
        }
    }

    override suspend fun subscribeForPushNotifications(
        identifier: String
    ): Either<Unit, Throwable> {
        return callApi {
            registerDevice(
                DeviceRegisterRequest(
                    DeviceRegisterDto(
                        platform = "android",
                        identifier = identifier,
                    )
                )
            )
        }
    }

    override suspend fun confirmPhoto(
        id: Long
    ): Either<Unit, Throwable> {
        return callApi {
            confirmPhoto(
                ConfirmPhotoRequest(
                    id = id,
                )
            )
        }
    }

    override suspend fun rejectPhoto(
        id: Long,
        reason: String
    ): Either<Unit, Throwable> {
        return callApi {
            rejectPhoto(
                RejectPhotoRequest(
                    id = id,
                    reason = reason,
                )
            )
        }
    }

    override suspend fun getChats(
        folderId: Long?,
        page: Int,
    ): Either<List<ChatItem>, Throwable> {
        return callApi {
            getChats(
                ChatListRequest(
                    offset = page * pageSize,
                    limit = pageSize,
                    filters = ChatListFilterDto(
                        query = null,
                        folderId = folderId,
                    )
                )
            )
        }.mapOnlyLeft {
            it.results.map {
                it.model()
            }
        }
    }

    override suspend fun getChatsForPages(
        folderId: Long?,
        pageCount: Int
    ): Either<List<ChatItem>, Throwable> {
        return callApi {
            getChats(
                ChatListRequest(
                    offset = 0,
                    limit = pageSize * pageCount,
                    filters = ChatListFilterDto(
                        query = null,
                        folderId = folderId,
                    )
                )
            )
        }.mapOnlyLeft {
            it.results.map {
                it.model()
            }
        }
    }

    override suspend fun getChatWithPatientMessages(
        userId: Long,
        offset: Int,
        count: Int,
        sinceDate: LocalDateTime?
    ): Either<MessageListPortion, Throwable> {
        return callApi {
            getChatWithPatientMessages(
                ChatWithPatientMessageListRequest(
                    userId = userId,
                    offset = offset,
                    limit = count,
                    filters = sinceDate?.let {
                        ChatMessageListFilterDto(
                            since = it
                        )
                    }
                )
            )
        }.mapOnlyLeft {
            it.model()
        }
    }

    override suspend fun sendMessageToChatWithPatient(
        userId: Long,
        message: String
    ): Either<Unit, Throwable> {
        return callApi {
            sendMessageToChatWithPatient(
                SendMessageToChatWithPatientRequest(
                    SendMessageToChatWithPatientDto(
                        recipientId = userId,
                        content = message,
                    )
                )
            )
        }
    }

    override suspend fun readMessagesInChatWithPatient(
        userId: Long,
        until: LocalDateTime
    ): Either<Unit, Throwable> {
        return callApi {
            readMessagesInChatWithPatient(
                ReadChatWithPatientMessagesRequest(
                    patientId = userId,
                    until = until,
                )
            )
        }
    }

    override suspend fun getChatWithDoctorMessages(
        offset: Int,
        count: Int,
        sinceDate: LocalDateTime?
    ): Either<MessageListPortion, Throwable> {
        return callApi {
            getChatWithDoctorMessages(
                ChatWithDoctorMessageListRequest(
                    offset = offset,
                    limit = count,
                    filters = sinceDate?.let {
                        ChatMessageListFilterDto(
                            since = it
                        )
                    }
                )
            )
        }.mapOnlyLeft {
            it.model()
        }
    }

    override suspend fun sendMessageToChatWithDoctor(
        message: String
    ): Either<Unit, Throwable> {
        return callApi {
            sendMessageToChatWithDoctor(
                SendMessageToChatWithDoctorRequest(
                    SendMessageToChatWithDoctorDto(
                        content = message,
                    )
                )
            )
        }
    }

    override suspend fun readMessagesInChatWithDoctor(
        until: LocalDateTime
    ): Either<Unit, Throwable> {
        return callApi {
            readMessagesInChatWithDoctor(
                ReadChatWithDoctorMessagesRequest(
                    until = until,
                )
            )
        }
    }

    override suspend fun getPatientPhotos(
    ): Either<List<Photo>, Throwable> {
        return callApi {
            getPatientPhotos(
                EmptyRequest()
            )
        }.mapOnlyLeft {
            it.results.mapNotNull {
                val state = when (it.status) {
                    "accepted" -> PhotoState.ACCEPTED
                    "rejected" -> PhotoState.REJECTED
                    "pending" -> PhotoState.PENDING
                    else -> {
                        logError("Received an unsupported photo status = ${it.status}")
                        null
                    }
                }
                if (state != null) {
                    Photo(
                        id = it.id,
                        imageUrl = it.file.original,
                        createdAt = it.createdAt,
                        state = state,
                        note = it.notes,
                    )
                } else null
            }
        }
    }

    override suspend fun deletePhoto(
        photoId: Long
    ): Either<Unit, Throwable> {
        return callApi {
            deletePhoto(
                DeletePhotoRequest(
                    id = photoId
                )
            )
        }
    }

    private suspend fun <R> callApi(
        action: suspend RestApi.() -> Response<R>
    ): Either<R, Throwable> {
        return callApi(
            action = action,
            unsuccessfulResponseMapper = {
                Either.Right(
                    IllegalStateException(
                        "Unsuccessful response received"
                    )
                )
            },
            notAuthorizedHandler = { Either.Right(it) },
            unknownErrorHandler = { Either.Right(it) },
        )
    }

    private suspend fun <R, E> callApi(
        action: suspend RestApi.() -> Response<R>,
        unsuccessfulResponseMapper: (Response<R>) -> Either<R, E>,
        notAuthorizedHandler: (NotAuthorizedException) -> Either<R, E>,
        unknownErrorHandler: (Throwable) -> Either<R, E>,
    ): Either<R, E> {
        return try {
            val response = action.invoke(restApi)
            if (response.isSuccessful) {
                Either.Left(response.body()!!)
            } else {
                unsuccessfulResponseMapper.invoke(response)
            }
        } catch (ex: NotAuthorizedException) {
            unauthorizedHandler.invoke()
            notAuthorizedHandler.invoke(ex)
        } catch (t: Throwable) {
            t.handleSkippingCancellation {
                unknownErrorHandler.invoke(t)
            }
        }
    }

}

class NotAuthorizedException : IOException()

private fun String.toGender() = when (this) {
    "male" -> Gender.MALE
    "female" -> Gender.FEMALE
    else -> Gender.MALE
}

private fun GetDoctorAccountResponse.model(
) = Doctor(
    doctor.user.model(),
    doctor.units.model()
)

private fun DoctorAccountUserDto.model(
) = DoctorAccount(
    fullName = fullName,
    email = email,
    phoneNumber = contactPhone,
    contactEmail = contactEmail,
    address = address,
    country = country,
    city = city,
    postalCode = postalCode,
    measurement = measurement
)

private fun PatientAccountDto.model(
) = PatientAccount(
    fullName = user.fullName,
    email = user.email,
    patientCode = code,
    weight = weight,
    height = height,
    phoneNumber = user.contactPhone,
    contactEmail = user.contactEmail,
    address = user.address,
    country = user.country,
    city = user.city,
    postalCode = user.postalCode,
    measurement = user.measurement,
    units = units.model()
)

private fun PatientAccountUpdateDto.model(
) = PatientAccountUpdated(
    fullName = user.fullName,
    email = user.email,
    patientCode = code,
    weight = weight,
    height = height,
    phoneNumber = user.contactPhone,
    contactEmail = user.contactEmail,
    address = user.address,
    country = user.country,
    city = user.city,
    postalCode = user.postalCode,
    measurement = user.measurement
)

private fun ChatItemDto.model(
) = ChatItem(
    id = id,
    fullName = fullName,
    avatarText = avatar.abbr,
    avatarBgColor = avatar.color,
    unreadCount = unreadCount,
    recentMessage = recentMessage?.let {
        ChatRecentMessage(
            id = it.id,
            createdAt = it.createdAt,
            senderId = it.senderId,
            content = it.content,
        )
    }
)

private fun ChatMessageListResponse.model(
) = MessageListPortion(
    totalCount = this.count,
    messages = this.results.map {
        ChatMessage(
            id = it.id,
            senderId = it.senderId,
            senderFullName = it.sender.fullName,
            createdAt = it.createdAt,
            content = it.content,
        )
    }
)

private fun UnitsDto.model(
) = Units(
    bmr = bmr,
    bmi = bmi,
    waistToHeight = waistToHeight,
    fm = fm,
    ffm = ffm,
    hip = hip,
    tbw = tbw,
    belly = belly,
    height = height,
    weight = weight,
    abdominalFm = abdominalFm
)