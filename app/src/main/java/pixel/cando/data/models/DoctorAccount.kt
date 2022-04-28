package pixel.cando.data.models

import pixel.cando.data.remote.dto.Units

data class Doctor(
    val account: DoctorAccount,
    val units: Units?
)

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
    val weight: Float,
    val height: String,
    val phoneNumber: String?,
    val contactEmail: String?,
    val address: String?,
    val country: String?,
    val city: String?,
    val postalCode: String?,
    val units: Units?
)