package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class ConfirmPhotoRequest(
    @Json(name = "id") val id: Long,
)

@JsonClass(generateAdapter = true)
data class RejectPhotoRequest(
    @Json(name = "id") val id: Long,
    @Json(name = "reason") val reason: String,
)

@JsonClass(generateAdapter = true)
data class UploadPhotoByDoctorRequest(
    @Json(name = "patientId") val patientId: Long,
    @Json(name = "photo") val weightHeight: UploadPhotoForPatientWeightHeightDto,
    @Json(name = "file") val photo: String,
)

@JsonClass(generateAdapter = true)
data class UploadPhotoByPatientRequest(
    @Json(name = "weight") val weight: Float,
    @Json(name = "file") val photo: String,
)

@JsonClass(generateAdapter = true)
data class UploadPhotoForPatientWeightHeightDto(
    @Json(name = "weight") val weight: Float,
    @Json(name = "heightText") val height: String,
)

@JsonClass(generateAdapter = true)
data class PhotoListResponse(
    @Json(name = "results") val results: List<PhotoDto>,
)

@JsonClass(generateAdapter = true)
data class PhotoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "createdAt") val createdAt: LocalDateTime,
    @Json(name = "status") val status: String,
    @Json(name = "notes") val notes: String?,
    @Json(name = "file") val file: PhotoFileDto,
)

@JsonClass(generateAdapter = true)
data class PhotoFileDto(
    @Json(name = "original") val original: String,
)


@JsonClass(generateAdapter = true)
data class DeletePhotoRequest(
    @Json(name = "id") val id: Long,
)
