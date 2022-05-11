package pixel.cando.data.remote.dto

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@JsonClass(generateAdapter = true)
data class GetDoctorAccountResponse(
    @Json(name = "doctor") val doctor: DoctorAccountDto
)

@JsonClass(generateAdapter = true)
data class UpdateDoctorAccountRequest(
    @Json(name = "doctor") val doctor: DoctorUpdateAccountDto
)

@JsonClass(generateAdapter = true)
data class UpdateDoctorAccountResponse(
    @Json(name = "doctor") val doctor: DoctorAccountDto
)

@JsonClass(generateAdapter = true)
data class DoctorAccountDto(
    @Json(name = "user") val user: DoctorAccountUserDto,
    @Json(name = "units") val units: UnitsDto
)

@JsonClass(generateAdapter = true)
data class DoctorUpdateAccountDto(
    @Json(name = "user") val user: DoctorAccountUserDto
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
    @Json(name = "measurement") val measurement: String?
)

@JsonClass(generateAdapter = true)
data class GetPatientAccountResponse(
    @Json(name = "patient") val patient: PatientAccountDto
)

@Parcelize
@JsonClass(generateAdapter = true)
data class UnitsDto(
    @Json(name = "bmr") val bmr: String,
    @Json(name = "bmi") val bmi: String,
    @Json(name = "waistToHeight") val waistToHeight: String?,
    @Json(name = "fm") val fm: String,
    @Json(name = "ffm") val ffm: String,
    @Json(name = "hip") val hip: String,
    @Json(name = "tbw") val tbw: String,
    @Json(name = "belly") val belly: String,
    @Json(name = "height") val height: String,
    @Json(name = "weight") val weight: String,
    @Json(name = "abdominalFm") val abdominalFm: String,
) : Parcelable

@JsonClass(generateAdapter = true)
data class UpdatePatientAccountRequest(
    @Json(name = "patient") val patient: UpdatePatientAccountDto
)

@JsonClass(generateAdapter = true)
data class UpdatePatientAccountResponse(
    @Json(name = "patient") val patient: PatientAccountUpdateDto
)

@JsonClass(generateAdapter = true)
data class PatientAccountDto(
    @Json(name = "user") val user: PatientAccountUserDto,
    @Json(name = "code") val code: String,
    @Json(name = "weight") val weight: Float,
    @Json(name = "heightText") val height: String,
    @Json(name = "units") val units: UnitsDto
)

@JsonClass(generateAdapter = true)
data class PatientAccountUpdateDto(
    @Json(name = "user") val user: PatientAccountUserDto,
    @Json(name = "code") val code: String,
    @Json(name = "weight") val weight: Float,
    @Json(name = "height") val height: String
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
    @Json(name = "measurement") val measurement: String,
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
    @Json(name = "measurement") val measurement: String?
)