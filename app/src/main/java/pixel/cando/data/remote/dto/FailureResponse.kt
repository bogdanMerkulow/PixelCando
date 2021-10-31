package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class FailureResponse(
    @Json(name = "exception") val exception: ExceptionBody
)

@JsonClass(generateAdapter = true)
internal data class ExceptionBody(
    @Json(name = "message") val message: String
)
