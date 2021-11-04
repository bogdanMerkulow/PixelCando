package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetAccountResponse(
    @Json(name = "doctor") val doctor: AccountDto
)

@JsonClass(generateAdapter = true)
data class UpdateAccountRequest(
    @Json(name = "doctor") val doctor: AccountDto
)

@JsonClass(generateAdapter = true)
data class UpdateAccountResponse(
    @Json(name = "doctor") val doctor: AccountDto
)

@JsonClass(generateAdapter = true)
data class AccountDto(
    @Json(name = "user") val user: AccountUserDto,
)

@JsonClass(generateAdapter = true)
data class AccountUserDto(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "contactEmail") val contactEmail: String?,
    @Json(name = "contactPhone") val contactPhone: String?,
    @Json(name = "address") val address: String?,
    @Json(name = "country") val country: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "postalCode") val postalCode: String?,
)