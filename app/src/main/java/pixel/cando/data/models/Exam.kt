package pixel.cando.data.models

import java.time.LocalDateTime

data class ExamListItemInfo(
    val id: Long,
    val createdAt: LocalDateTime,
    val number: Int,
    val bmi: Float,
)

data class ExamSingleItemInfo(
    val id: Long,
    val createdAt: LocalDateTime,
    val number: Int,
    val weight: Float,
    val bmi: Float,
    val bmr: Float,
    val fm: Float,
    val ffm: Float,
    val abdominalFatMass: Float,
    val tbw: Float,
    val hip: Float,
    val belly: Float,
    val waistToHeight: Float,
    val silhouetteUrl: String?,
)