package pixel.cando.data.models

data class Account(
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val contactEmail: String?,
    val address: String?,
    val country: String?,
)