package pixel.cando.data.models

data class PatientListItemInfo(
    val id: Long,
    val fullName: String,
    val gender: Gender,
    val age: Int,
    val avatarText: String,
    val avatarBgColor: String,
)

data class PatientSingleItemInfo(
    val id: Long,
    val fullName: String,
)