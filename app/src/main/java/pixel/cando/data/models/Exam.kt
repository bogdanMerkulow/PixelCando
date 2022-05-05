package pixel.cando.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

data class ExamListItemInfo(
    val id: Long,
    val createdAt: LocalDateTime,
    val number: Int,
    val weight: Float,
    val fatMass: Float,
    val fatFreeMass: Float,
    val abdominalFatMass: Float,
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

@Parcelize
data class ExamUnits(
    val bmr: String,
    val bmi: String,
    val waistToHeight: String?,
    val fm: String,
    val ffm: String,
    val hip: String,
    val tbw: String,
    val belly: String,
    val height: String,
    val weight: String,
    val abdominalFm: String,
) : Parcelable