package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceRegisterRequest(
    @Json(name = "device") val device: DeviceRegisterDto,
)

@JsonClass(generateAdapter = true)
data class DeviceRegisterDto(
    @Json(name = "platform") val platform: String,
    @Json(name = "identifier") val identifier: String,
)
