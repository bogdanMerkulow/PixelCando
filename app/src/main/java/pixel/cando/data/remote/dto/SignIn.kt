package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignInRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
)

@JsonClass(generateAdapter = true)
data class SignInResponse(
    @Json(name = "doctor") val doctor: SignedInUserWrapperDto?,
    @Json(name = "patient") val patient: SignedInUserWrapperDto?,
)

@JsonClass(generateAdapter = true)
data class SignedInUserWrapperDto(
    @Json(name = "user") val user: SignedInUserDto
)

@JsonClass(generateAdapter = true)
data class SignedInUserDto(
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "role") val role: String,
)
