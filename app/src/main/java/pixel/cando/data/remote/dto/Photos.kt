package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadPhotoForPatientRequest(
    @Json(name = "patientId") val patientId: Long,
    @Json(name = "file") val photo: String,
)