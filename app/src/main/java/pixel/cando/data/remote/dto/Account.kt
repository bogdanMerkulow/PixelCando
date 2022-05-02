package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetDoctorAccountResponse(
    @Json(name = "doctor") val doctor: DoctorAccountDto
)

@JsonClass(generateAdapter = true)
data class UpdateDoctorAccountRequest(
    @Json(name = "doctor") val doctor: DoctorAccountDto
)

@JsonClass(generateAdapter = true)
data class UpdateDoctorAccountResponse(
    @Json(name = "doctor") val doctor: DoctorAccountDto
)

@JsonClass(generateAdapter = true)
data class DoctorAccountDto(
    @Json(name = "user") val user: DoctorAccountUserDto,
)

@JsonClass(generateAdapter = true)
data class DoctorAccountUserDto(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "contactEmail") val contactEmail: String?,
    @Json(name = "contactPhone") val contactPhone: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "postalCode") val postalCode: String?,
)

@JsonClass(generateAdapter = true)
data class GetPatientAccountResponse(
    @Json(name = "patient") val patient: PatientAccountDto
)

@JsonClass(generateAdapter = true)
data class UpdatePatientAccountRequest(
    @Json(name = "patient") val patient: UpdatePatientAccountDto
)

@JsonClass(generateAdapter = true)
data class UpdatePatientAccountResponse(
    @Json(name = "patient") val patient: PatientAccountDto
)

@JsonClass(generateAdapter = true)
data class PatientAccountDto(
    @Json(name = "user") val user: PatientAccountUserDto,
    @Json(name = "code") val code: String,
    @Json(name = "weight") val weight: Float,
    @Json(name = "heightMetric") val height: Float,
)

@JsonClass(generateAdapter = true)
data class PatientAccountUserDto(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "contactEmail") val contactEmail: String?,
    @Json(name = "contactPhone") val contactPhone: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "postalCode") val postalCode: String?,
)

@JsonClass(generateAdapter = true)
data class UpdatePatientAccountDto(
    @Json(name = "user") val user: UpdatePatientAccountUserDto,
)

@JsonClass(generateAdapter = true)
data class UpdatePatientAccountUserDto(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "contactEmail") val contactEmail: String?,
    @Json(name = "contactPhone") val contactPhone: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "postalCode") val postalCode: String?,
)