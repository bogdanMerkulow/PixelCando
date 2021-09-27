package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PatientListFilterDto(
    @Json(name = "query") val query: String?,
    @Json(name = "folderId") val folderId: Long?,
)