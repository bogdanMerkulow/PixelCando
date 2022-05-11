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
    val weight: String,
    val bmi: String,
    val bmr: String,
    val fm: String,
    val ffm: String,
    val abdominalFatMass: String,
    val tbw: String,
    val hip: String,
    val belly: String,
    val waistToHeight: String,
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