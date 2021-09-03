package pixel.cando.data.remote

import pixel.cando.data.models.PatientBriefInfo
import pixel.cando.data.remote.dto.PatientListRequest
import pixel.cando.data.remote.dto.QueryFilterDto
import pixel.cando.data.remote.dto.UploadPhotoRequest
import pixel.cando.utils.Either
import pixel.cando.utils.mapOnlyLeft
import retrofit2.Response
import java.io.IOException

interface RemoteRepository {

    suspend fun getPatients(
        page: Int,
    ): Either<List<PatientBriefInfo>, Throwable>

    suspend fun uploadPhoto(
        photo: String
    ): Either<Unit, Throwable>

}

class RealRemoteRepository(
    private val restApi: RestApi,
    private val unauthorizedHandler: () -> Unit,
) : RemoteRepository {

    private val pageSize = 20

    override suspend fun getPatients(
        page: Int,
    ): Either<List<PatientBriefInfo>, Throwable> {
        return callApi {
            restApi.getPatients(
                PatientListRequest(
                    offset = page * pageSize,
                    limit = pageSize,
                    filter = QueryFilterDto(
                        query = ""
                    )
                )
            )
        }.mapOnlyLeft {
            it.results.map {
                PatientBriefInfo(
                    id = it.userId,
                    fullName = it.user.fullName,
                )
            }
        }
    }

    override suspend fun uploadPhoto(
        photo: String
    ): Either<Unit, Throwable> {
        return callApi {
            restApi.uploadPhoto(
                UploadPhotoRequest(
                    photo = photo,
                )
            )
        }
    }

    private suspend fun <R> callApi(
        action: suspend () -> Response<R>
    ): Either<R, Throwable> {
        return try {
            val response = action.invoke()
            if (response.isSuccessful) {
                Either.Left(response.body()!!)
            } else {
                Either.Right(
                    IllegalStateException(
                        "Unsuccessful response received"
                    )
                )
            }
        } catch (ex: NotAuthorizedException) {
            unauthorizedHandler.invoke()
            Either.Right(ex)
        } catch (ex: Throwable) {
            Either.Right(ex)
        }
    }

}

class NotAuthorizedException : IOException()