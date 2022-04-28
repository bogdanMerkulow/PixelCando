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
    @Json(name = "units") val units: Units?
)

@JsonClass(generateAdapter = true)
data class GetPatientAccountResponse(
    @Json(name = "patient") val patient: PatientAccountDto
)

@Parcelize
@JsonClass(generateAdapter = true)
data class Units(
    @Json(name = "bmr") val bmr: String?,
    @Json(name = "bmi") val bmi: String?,
    @Json(name = "fmPct") val fmPct: String?,
    @Json(name = "ffmPct") val ffmPct: String?,
    @Json(name = "tbwPct") val tbwPct: String?,
    @Json(name = "eyePos") val eyePos: String?,
    @Json(name = "anklePos") val anklePos: String?,
    @Json(name = "totalEnergy1") val totalEnergy1: String?,
    @Json(name = "totalEnergy2") val totalEnergy2: String?,
    @Json(name = "totalEnergy3") val totalEnergy3: String?,
    @Json(name = "totalEnergy4") val totalEnergy4: String?,
    @Json(name = "totalEnergy5") val totalEnergy5: String?,
    @Json(name = "totalEnergy6") val totalEnergy6: String?,
    @Json(name = "waistToHeight") val waistToHeight: String?,
    @Json(name = "fm") val fm: String?,
    @Json(name = "ecw") val ecw: String?,
    @Json(name = "ffm") val ffm: String?,
    @Json(name = "fmi") val fmi: String?,
    @Json(name = "hip") val hip: String?,
    @Json(name = "icw") val icw: String?,
    @Json(name = "leg") val leg: String?,
    @Json(name = "tbw") val tbw: String?,
    @Json(name = "calf") val calf: String?,
    @Json(name = "ffmi") val ffmi: String?,
    @Json(name = "foot") val foot: String?,
    @Json(name = "belly") val belly: String?,
    @Json(name = "chest") val chest: String?,
    @Json(name = "thigh") val thigh: String?,
    @Json(name = "trunk") val trunk: String?,
    @Json(name = "height") val height: String?,
    @Json(name = "weight") val weight: String?,
    @Json(name = "abdominalFm") val abdominalFm: String?,
) : Parcelable

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
    @Json(name = "height") val height: String,
    @Json(name = "units") val units: Units
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