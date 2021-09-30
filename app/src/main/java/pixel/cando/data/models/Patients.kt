package pixel.cando.data.models

import java.time.LocalDateTime

data class PatientListItemInfo(
    val id: Long,
    val fullName: String,
    val gender: Gender,
    val age: Int,
    val avatarText: String,
    val avatarBgColor: String,
    val lastExamAt: LocalDateTime?,
)

data class PatientSingleItemInfo(
    val id: Long,
    val fullName: String,
)