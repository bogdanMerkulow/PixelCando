package pixel.cando.ui.main.patient_profile

import android.os.Parcelable
import android.util.Patterns
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.local.SessionWiper
import pixel.cando.data.models.PatientAccount
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui.main.doctor_profile.DoctorProfileEvent
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight

object PatientProfileLogic {

    fun init(
        model: PatientProfileDataModel
    ): First<PatientProfileDataModel, PatientProfileEffect> {
        if (model.account == null) {
            return First.first(
                model.copy(
                    isLoading = true,
                ),
                setOf(
                    PatientProfileEffect.LoadAccount
                )
            )
        }
        return First.first(model)
    }

    fun update(
        model: PatientProfileDataModel,
        event: PatientProfileEvent
    ): Next<PatientProfileDataModel, PatientProfileEffect> {
        return when (event) {
            // ui
            is PatientProfileEvent.FullNameChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            fullName = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.EmailChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            email = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.MeasurementChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            measurement = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.PhoneNumberChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            phoneNumber = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.ContactEmailChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            contactEmail = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.AddressChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            address = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.CityChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            city = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.PostalCodeChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            postalCode = event.value
                        )
                    )
                )
            }
            is PatientProfileEvent.SaveTap -> {
                val account = model.account
                if (account != null
                    && model.isLoading.not()
                ) {
                    Next.next(
                        model.copy(
                            isLoading = true,
                        ),
                        setOf(
                            PatientProfileEffect.UpdateAccount(account)
                        )
                    )
                } else Next.noChange()
            }
            is PatientProfileEvent.LogoutTap -> {
                Next.dispatch(
                    setOf(
                        PatientProfileEffect.AskToConfirmLogout
                    )
                )
            }
            is PatientProfileEvent.LogoutConfirmed -> {
                Next.dispatch(
                    setOf(
                        PatientProfileEffect.Logout
                    )
                )
            }
            // model
            is PatientProfileEvent.LoadAccountSuccess -> {
                Next.next(
                    model.copy(
                        account = event.account,
                        isLoading = false,
                    )
                )
            }
            is PatientProfileEvent.LoadAccountFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        PatientProfileEffect.ShowUnexpectedError
                    )
                )
            }
            is PatientProfileEvent.UpdateAccountSuccess -> {
                Next.next(
                    model.copy(
                        account = event.account,
                        isLoading = false,
                    )
                )
            }
            is PatientProfileEvent.UpdateAccountFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        PatientProfileEffect.ShowUnexpectedError
                    )
                )
            }
        }
    }

    fun effectHandler(
        sessionWiper: SessionWiper,
        rootRouter: RootRouter,
        remoteRepository: RemoteRepository,
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        logoutConfirmationAsker: () -> Unit,
    ): Connectable<PatientProfileEffect, PatientProfileEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PatientProfileEffect.LoadAccount -> {
                    val result = remoteRepository.getPatientAccount()
                    result.onLeft {
                        output.accept(
                            PatientProfileEvent.LoadAccountSuccess(
                                it.dataModel
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            PatientProfileEvent.LoadAccountFailure
                        )
                    }
                }
                is PatientProfileEffect.UpdateAccount -> {
                    val result = remoteRepository.updatePatientAccount(
                        fullName = effect.account.fullName,
                        email = effect.account.email,
                        phoneNumber = effect.account.phoneNumber,
                        contactEmail = effect.account.contactEmail,
                        address = effect.account.address,
                        city = effect.account.city,
                        postalCode = effect.account.postalCode,
                        measurement = effect.account.measurement
                    )
                    result.onLeft {
                        output.accept(
                            PatientProfileEvent.UpdateAccountSuccess(
                                it.dataModel
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            PatientProfileEvent.UpdateAccountFailure
                        )
                    }
                }
                is PatientProfileEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
                is PatientProfileEffect.AskToConfirmLogout -> {
                    logoutConfirmationAsker.invoke()
                }
                is PatientProfileEffect.Logout -> {
                    sessionWiper.wipe()
                    rootRouter.replaceScreen(
                        Screens.authFlow()
                    )
                }
            }
        }

    fun initialModel(
    ) = PatientProfileDataModel(
        account = null,
        isLoading = false,
    )

}

sealed class PatientProfileEvent {
    // ui
    data class FullNameChanged(
        val value: String
    ) : PatientProfileEvent()

    data class EmailChanged(
        val value: String
    ) : PatientProfileEvent()

    data class MeasurementChanged(
        val value: String
    ) : PatientProfileEvent()

    data class PhoneNumberChanged(
        val value: String
    ) : PatientProfileEvent()

    data class ContactEmailChanged(
        val value: String
    ) : PatientProfileEvent()

    data class AddressChanged(
        val value: String
    ) : PatientProfileEvent()

    data class CityChanged(
        val value: String
    ) : PatientProfileEvent()

    data class PostalCodeChanged(
        val value: String
    ) : PatientProfileEvent()

    object SaveTap : PatientProfileEvent()

    object LogoutTap : PatientProfileEvent()

    object LogoutConfirmed : PatientProfileEvent()

    // model
    data class LoadAccountSuccess(
        val account: AccountDataModel
    ) : PatientProfileEvent()

    object LoadAccountFailure : PatientProfileEvent()

    data class UpdateAccountSuccess(
        val account: AccountDataModel
    ) : PatientProfileEvent()

    object UpdateAccountFailure : PatientProfileEvent()

}

sealed class PatientProfileEffect {
    object AskToConfirmLogout : PatientProfileEffect()
    object Logout : PatientProfileEffect()
    object LoadAccount : PatientProfileEffect()

    data class UpdateAccount(
        val account: AccountDataModel
    ) : PatientProfileEffect()

    object ShowUnexpectedError : PatientProfileEffect()
}

@Parcelize
data class PatientProfileDataModel(
    val account: AccountDataModel?,
    val isLoading: Boolean,
) : Parcelable

@Parcelize
data class AccountDataModel(
    val fullName: String,
    val email: String,
    val patientCode: String,
    val phoneNumber: String?,
    val contactEmail: String?,
    val address: String?,
    val country: String?,
    val city: String?,
    val postalCode: String?,
    val measurement: String?
) : Parcelable

data class PatientProfileViewModel(
    val fields: ProfileFieldListViewModel?,
    val isLoaderVisible: Boolean,
    val isContentVisible: Boolean,
    val maySave: Boolean,
)

data class ProfileFieldListViewModel(
    val fullNameField: ProfileFieldViewModel,
    val emailField: ProfileFieldViewModel,
    val patientCodeField: ProfileFieldViewModel,
    val phoneNumberField: ProfileFieldViewModel,
    val contactEmailField: ProfileFieldViewModel,
    val addressField: ProfileFieldViewModel,
    val countryField: ProfileFieldViewModel,
    val cityField: ProfileFieldViewModel,
    val postalCodeField: ProfileFieldViewModel,
    val measurement: ProfileFieldViewModel
)

data class ProfileFieldViewModel(
    val value: String?,
    val error: String?
)

fun PatientProfileDataModel.viewModel(
    resourceProvider: ResourceProvider,
) = PatientProfileViewModel(
    fields = account?.let {
        ProfileFieldListViewModel(
            fullNameField = ProfileFieldViewModel(
                value = it.fullName,
                error = if (it.isFullNameValid) null
                else resourceProvider.getString(R.string.profile_full_name_cannot_be_empty)
            ),
            emailField = ProfileFieldViewModel(
                value = it.email,
                error = if (it.isEmailValid) null
                else resourceProvider.getString(R.string.invalid_email)
            ),
            patientCodeField = ProfileFieldViewModel(
                value = it.patientCode,
                error = null
            ),

            phoneNumberField = ProfileFieldViewModel(
                value = it.phoneNumber,
                error = null
            ),
            contactEmailField = ProfileFieldViewModel(
                value = it.contactEmail,
                error = if (it.isContactEmailValid) null
                else resourceProvider.getString(R.string.invalid_email)
            ),
            addressField = ProfileFieldViewModel(
                value = it.address,
                error = null
            ),
            countryField = ProfileFieldViewModel(
                value = it.country,
                error = null
            ),
            cityField = ProfileFieldViewModel(
                value = it.city,
                error = null
            ),
            postalCodeField = ProfileFieldViewModel(
                value = it.postalCode,
                error = null
            ),
            measurement = ProfileFieldViewModel(
                value = it.measurement,
                error = null
            )
        )
    },
    isLoaderVisible = isLoading,
    isContentVisible = isLoading.not() && account != null,
    maySave = account?.let { it.maySave && isLoading.not() } ?: false,
)

private val AccountDataModel.isFullNameValid: Boolean
    get() = fullName.isNotBlank()

private val AccountDataModel.isEmailValid: Boolean
    get() = email.isNotBlank()
            && Patterns.EMAIL_ADDRESS.matcher(email).matches()

private val AccountDataModel.isContactEmailValid: Boolean
    get() = contactEmail.isNullOrBlank()
            || Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()

private val AccountDataModel.maySave: Boolean
    get() = isFullNameValid
            && isEmailValid
            && isContactEmailValid

private val PatientAccount.dataModel: AccountDataModel
    get() = AccountDataModel(
        fullName = fullName,
        email = email,
        patientCode = patientCode,
        phoneNumber = phoneNumber,
        contactEmail = contactEmail,
        address = address,
        country = country,
        city = city,
        postalCode = postalCode,
        measurement = measurement
    )