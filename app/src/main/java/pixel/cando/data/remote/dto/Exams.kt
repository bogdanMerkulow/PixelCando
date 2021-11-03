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
    @Json(name = "results") val results: List<ExamBriefDto>,
)

@JsonClass(generateAdapter = true)
data class GetExamRequest(
    @Json(name = "id") val id: Long,
)

@JsonClass(generateAdapter = true)
data class GetExamResponse(
    @Json(name = "exam") val exam: ExamDto,
)

@JsonClass(generateAdapter = true)
data class ExamBriefDto(
    @Json(name = "id") val id: Long,
    @Json(name = "createdAt") val createdAt: LocalDateTime,
    @Json(name = "no") val no: Int,
    @Json(name = "params") val params: ExamParamsBriefDto,
)

@JsonClass(generateAdapter = true)
data class ExamParamsBriefDto(
    @Json(name = "bmi") val bmi: Float,
)

@JsonClass(generateAdapter = true)
data class ExamDto(
    @Json(name = "id") val id: Long,
    @Json(name = "createdAt") val createdAt: LocalDateTime,
    @Json(name = "no") val no: Int,
    @Json(name = "params") val params: ExamParamsDto,
    @Json(name = "silhouette") val silhouette: String?,
)

@JsonClass(generateAdapter = true)
data class ExamParamsDto(
    @Json(name = "weight") val weight: Float,
    @Json(name = "bmi") val bmi: Float,
    @Json(name = "bmr") val bmr: Float,
    @Json(name = "fm") val fm: Float,
    @Json(name = "ffm") val ffm: Float,
    @Json(name = "abdominalFm") val abdominalFm: Float,
    @Json(name = "tbw") val tbw: Float,
    @Json(name = "hip") val hip: Float,
    @Json(name = "belly") val belly: Float,
    @Json(name = "waistToHeight") val waistToHeight: Float,
)