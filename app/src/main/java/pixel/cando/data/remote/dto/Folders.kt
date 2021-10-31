package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FolderListRequest(
    @Json(name = "offset") val offset: Int,
    @Json(name = "limit") val limit: Int,
)

@JsonClass(generateAdapter = true)
data class FolderListResponse(
    @Json(name = "results") val folders: List<FolderDto>
)

@JsonClass(generateAdapter = true)
data class FolderDto(
    @Json(name = "id") val id: Long,
    @Json(name = "title") val title: String,
)