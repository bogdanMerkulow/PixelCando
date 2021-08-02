package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class PatientListRequest(
    @Json(name = "offset") val offset: Int,
    @Json(name = "limit") val limit: Int,
    @Json(name = "filters") val filter: QueryFilterDto
)

@JsonClass(generateAdapter = true)
data class PatientListResponse(
    @Json(name = "results") val results: List<PatientDto>,
)

@JsonClass(generateAdapter = true)
data class PatientDto(
    @Json(name = "userId") val userId: Long,
    @Json(name = "dateOfBirth") val dateOfBirth: LocalDate,
    @Json(name = "gender") val gender: String,
    @Json(name = "height") val height: Float,
    @Json(name = "weight") val weight: Float,
    @Json(name = "user") val user: PatientUserDto,
)

@JsonClass(generateAdapter = true)
data class PatientUserDto(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "contactEmail") val contactEmail: String,
    @Json(name = "contactPhone") val contactPhone: String,
)
