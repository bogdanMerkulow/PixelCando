package pixel.cando.data.remote

import com.squareup.moshi.Moshi
import pixel.cando.data.models.Folder
import pixel.cando.data.models.Gender
import pixel.cando.data.models.PatientBriefInfo
import pixel.cando.data.models.UploadPhotoFailure
import pixel.cando.data.remote.dto.FolderListRequest
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
    ): Either<List<PatientBriefInfo>, Throwable>

    suspend fun getFolders(
    ): Either<List<Folder>, Throwable>

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
    ): Either<List<PatientBriefInfo>, Throwable> {
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
                PatientBriefInfo(
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
                )
            }
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