package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class PatientListRequest(
    @Json(name = "offset") val offset: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "filters") val filter: PatientListFilterDto
)

@JsonClass(generateAdapter = true)
data class PatientListFilterDto(
    @Json(name = "query") val query: String?,
    @Json(name = "folderId") val folderId: Long?,
)

@JsonClass(generateAdapter = true)
data class PatientListResponse(
    @Json(name = "results") val results: List<PatientDto>,
)

@JsonClass(generateAdapter = true)
data class PatientGetRequest(
    @Json(name = "id") val id: Long,
)

@JsonClass(generateAdapter = true)
data class PatientGetResponse(
    @Json(name = "patient") val patient: PatientDto,
)

@JsonClass(generateAdapter = true)
data class PatientDto(
    @Json(name = "userId") val userId: Long,
    @Json(name = "dateOfBirth") val dateOfBirth: LocalDate,
    @Json(name = "gender") val gender: String,
    @Json(name = "height") val height: String,
    @Json(name = "weight") val weight: Float,
    @Json(name = "age") val age: Int,
    @Json(name = "user") val user: PatientUserDto,
    @Json(name = "photo") val photo: PatientPhotoDto?,
    @Json(name = "lastExamAt") val lastExamAt: LocalDateTime?
)

@JsonClass(generateAdapter = true)
data class PatientUserDto(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "contactEmail") val contactEmail: String?,
    @Json(name = "contactPhone") val contactPhone: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "postalCode") val postalCode: String?,
    @Json(name = "avatar") val avatar: PatientUserAvatarDto,
)

@JsonClass(generateAdapter = true)
data class PatientUserAvatarDto(
    @Json(name = "color") val color: String,
    @Json(name = "abbr") val text: String,
)

@JsonClass(generateAdapter = true)
data class PatientPhotoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "createdAt") val createdAt: LocalDateTime,
    @Json(name = "file") val file: PatientPhotoFileDto,
)

@JsonClass(generateAdapter = true)
data class PatientPhotoFileDto(
    @Json(name = "original") val original: String
)