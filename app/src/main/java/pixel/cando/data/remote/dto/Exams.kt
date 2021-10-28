package pixel.cando.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class ExamListRequest(
    @Json(name = "patientId") val patientId: Long,
    @Json(name = "offset") val offset: Int,
    @Json(name = "limit") val limit: Int,
)

@JsonClass(generateAdapter = true)
data class ExamListResponse(
    @Json(name = "results") val results: List<ExamDto>,
)

@JsonClass(generateAdapter = true)
data class ExamDto(
    @Json(name = "id") val id: Long,
    @Json(name = "createdAt") val createdAt: LocalDateTime,
    @Json(name = "no") val no: Int,
    @Json(name = "params") val params: ExamParamsDto,
)

@JsonClass(generateAdapter = true)
data class ExamParamsDto(
    @Json(name = "bmi") val bmi: Float,
)