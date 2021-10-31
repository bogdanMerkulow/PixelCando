package pixel.cando.data.remote

import com.squareup.moshi.Moshi
import pixel.cando.data.remote.dto.FailureResponse
import pixel.cando.utils.objectFromJson
import retrofit2.Response

fun <R> Response<R>.errorMessage(
    moshi: Moshi,
): String? {
    if (isSuccessful.not()) {
        val failureResponse: FailureResponse? = moshi.objectFromJson(
            errorBody()!!.string()
        )
        return failureResponse?.exception?.message
    }
    return null
}
