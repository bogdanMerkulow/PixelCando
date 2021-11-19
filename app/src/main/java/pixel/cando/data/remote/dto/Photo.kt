package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfirmPhotoRequest(
    @Json(name = "id") val id: Long,
)

@JsonClass(generateAdapter = true)
data class RejectPhotoRequest(
    @Json(name = "id") val id: Long,
    @Json(name = "reason") val reason: String,
)