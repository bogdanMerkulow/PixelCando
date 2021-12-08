package pixel.cando.data.models

data class DoctorAccount(
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val contactEmail: String?,
    val address: String?,
    val country: String?,
    val city: String?,
    val postalCode: String?,
)

data class PatientAccount(
    val fullName: String,
    val email: String,
    val patientCode: String,
    val phoneNumber: String?,
    val contactEmail: String?,
    val address: String?,
    val country: String?,
    val city: String?,
    val postalCode: String?,
)