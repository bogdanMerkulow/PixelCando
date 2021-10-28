package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadPhotoForPatientRequest(
    @Json(name = "patientId") val patientId: Long,
    @Json(name = "photo") val weightHeight: UploadPhotoForPatientWeightHeightDto,
    @Json(name = "file") val photo: String,
)

@JsonClass(generateAdapter = true)
data class UploadPhotoForPatientWeightHeightDto(
    @Json(name = "weight") val weight: Float,
    @Json(name = "height") val height: Float,
)