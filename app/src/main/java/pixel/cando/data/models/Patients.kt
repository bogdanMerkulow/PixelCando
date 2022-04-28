package pixel.cando.data.models

import pixel.cando.data.remote.dto.Units
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
    val gender: Gender,
    val age: Int,
    val weight: Float,
    val height: String,
    val phoneNumber: String?,
    val email: String?,
    val address: String?,
    val country: String?,
    val city: String?,
    val postalCode: String?,
    val photoToReview: PatientPhotoToReview?,
    val units: Units?
)

data class PatientPhotoToReview(
    val id: Long,
    val createdAt: LocalDateTime,
    val url: String,
)