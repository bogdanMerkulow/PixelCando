package pixel.cando.data.remote

import com.squareup.moshi.Moshi
import pixel.cando.data.models.Exam
import pixel.cando.data.models.Folder
import pixel.cando.data.models.Gender
import pixel.cando.data.models.PatientListItemInfo
import pixel.cando.data.models.PatientSingleItemInfo
import pixel.cando.data.models.UploadPhotoFailure
import pixel.cando.data.remote.dto.ExamListRequest
import pixel.cando.data.remote.dto.FolderListRequest
import pixel.cando.data.remote.dto.PatientGetRequest
import pixel.cando.data.remote.dto.PatientListFilterDto
import pixel.cando.data.remote.dto.PatientListRequest
import pixel.cando.data.remote.dto.UploadPhotoForPatientRequest
import pixel.cando.utils.Either
import pixel.cando.utils.mapOnlyLeft
import retrofit2.Response
import java.io.IOException

interface RemoteRepository {

    suspend fun getPatients(
        folderId: Long?,
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
    ): Either<List<Exam>, Throwable>

    suspend fun uploadPhoto(
        patientId: Long,
        photo: String
    ): Either<Unit, UploadPhotoFailure>

}

class RealRemoteRepository(
    private val restApi: RestApi,
    private val moshi: Moshi,
    private val unauthorizedHandler: () -> Unit,
) : RemoteRepository {

    private val pageSize = 20

    override suspend fun getPatients(
        folderId: Long?,
        page: Int,
    ): Either<List<PatientListItemInfo>, Throwable> {
        return callApi {
            getPatients(
                PatientListRequest(
                    offset = page * pageSize,
                    limit = pageSize,
                    filter = PatientListFilterDto(
                        query = "",
                        folderId = folderId,
                    )
                )
            )
        }.mapOnlyLeft {
            it.results.map {
                PatientListItemInfo(
                    id = it.userId,
                    fullName = it.user.fullName,
                    gender = it.gender.let {
                        when (it) {
                            "male" -> Gender.MALE
                            "female" -> Gender.FEMALE
                            else -> Gender.MALE
                        }
                    },
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
    ): Either<List<Exam>, Throwable> {
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
                Exam(
                    id = it.id,
                    createdAt = it.createdAt,
                    number = it.no,
                    bmi = it.bmi,
                )
            }
        }
    }

    override suspend fun uploadPhoto(
        patientId: Long,
        photo: String,
    ): Either<Unit, UploadPhotoFailure> {
        return callApi(
            action = {
                uploadPhoto(
                    UploadPhotoForPatientRequest(
                        patientId = patientId,
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
        } catch (ex: Throwable) {
            unknownErrorHandler.invoke(ex)
        }
    }

}

class NotAuthorizedException : IOException()