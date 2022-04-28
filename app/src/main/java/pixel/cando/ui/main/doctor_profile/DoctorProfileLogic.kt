package pixel.cando.ui.main.doctor_profile

import android.os.Parcelable
import android.util.Patterns
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.local.SessionWiper
import pixel.cando.data.models.DoctorAccount
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight

object DoctorProfileLogic {

    fun init(
        model: DoctorProfileDataModel
    ): First<DoctorProfileDataModel, DoctorProfileEffect> {
        if (model.account == null) {
            return First.first(
                model.copy(
                    isLoading = true,
                ),
                setOf(
                    DoctorProfileEffect.LoadAccount
                )
            )
        }
        return First.first(model)
    }

    fun update(
        model: DoctorProfileDataModel,
        event: DoctorProfileEvent
    ): Next<DoctorProfileDataModel, DoctorProfileEffect> {
        return when (event) {
            // ui
            is DoctorProfileEvent.FullNameChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            fullName = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.EmailChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            email = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.MeasurementChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            measurement = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.PhoneNumberChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            phoneNumber = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.ContactEmailChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            contactEmail = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.AddressChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            address = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.CityChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            city = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.PostalCodeChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            postalCode = event.value
                        )
                    )
                )
            }
            is DoctorProfileEvent.SaveTap -> {
                val account = model.account
                if (account != null
                    && model.isLoading.not()
                ) {
                    Next.next(
                        model.copy(
                            isLoading = true,
                        ),
                        setOf(
                            DoctorProfileEffect.UpdateAccount(account)
                        )
                    )
                } else Next.noChange()
            }
            is DoctorProfileEvent.LogoutTap -> {
                Next.dispatch(
                    setOf(
                        DoctorProfileEffect.AskToConfirmLogout
                    )
                )
            }
            is DoctorProfileEvent.LogoutConfirmed -> {
                Next.dispatch(
                    setOf(
                        DoctorProfileEffect.Logout
                    )
                )
            }
            // model
            is DoctorProfileEvent.LoadAccountSuccess -> {
                Next.next(
                    model.copy(
                        account = event.account,
                        isLoading = false,
                    )
                )
            }
            is DoctorProfileEvent.LoadAccountFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        DoctorProfileEffect.ShowUnexpectedError
                    )
                )
            }
            is DoctorProfileEvent.UpdateAccountSuccess -> {
                Next.next(
                    model.copy(
                        account = event.account,
                        isLoading = false,
                    )
                )
            }
            is DoctorProfileEvent.UpdateAccountFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        DoctorProfileEffect.ShowUnexpectedError
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
    ): Connectable<DoctorProfileEffect, DoctorProfileEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is DoctorProfileEffect.LoadAccount -> {
                    val result = remoteRepository.getDoctorAccount()
                    result.onLeft {
                        output.accept(
                            DoctorProfileEvent.LoadAccountSuccess(
                                it.dataModel
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            DoctorProfileEvent.LoadAccountFailure
                        )
                    }
                }
                is DoctorProfileEffect.UpdateAccount -> {
                    val result = remoteRepository.updateDoctorAccount(
                        effect.account.model
                    )
                    result.onLeft {
                        output.accept(
                            DoctorProfileEvent.UpdateAccountSuccess(
                                it.dataModel
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            DoctorProfileEvent.UpdateAccountFailure
                        )
                    }
                }
                is DoctorProfileEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
                is DoctorProfileEffect.AskToConfirmLogout -> {
                    logoutConfirmationAsker.invoke()
                }
                is DoctorProfileEffect.Logout -> {
                    sessionWiper.wipe()
                    rootRouter.replaceScreen(
                        Screens.authFlow()
                    )
                }
            }
        }

    fun initialModel(
    ) = DoctorProfileDataModel(
        account = null,
        isLoading = false,
    )

}

sealed class DoctorProfileEvent {
    // ui
    data class FullNameChanged(
        val value: String
    ) : DoctorProfileEvent()

    data class EmailChanged(
        val value: String
    ) : DoctorProfileEvent()

    data class MeasurementChanged(
        val value: String
    ) : DoctorProfileEvent()

    data class PhoneNumberChanged(
        val value: String
    ) : DoctorProfileEvent()

    data class ContactEmailChanged(
        val value: String
    ) : DoctorProfileEvent()

    data class AddressChanged(
        val value: String
    ) : DoctorProfileEvent()

    data class CityChanged(
        val value: String
    ) : DoctorProfileEvent()

    data class PostalCodeChanged(
        val value: String
    ) : DoctorProfileEvent()

    object SaveTap : DoctorProfileEvent()

    object LogoutTap : DoctorProfileEvent()
    object LogoutConfirmed : DoctorProfileEvent()

    // model
    data class LoadAccountSuccess(
        val account: AccountDataModel
    ) : DoctorProfileEvent()

    object LoadAccountFailure : DoctorProfileEvent()

    data class UpdateAccountSuccess(
        val account: AccountDataModel
    ) : DoctorProfileEvent()

    object UpdateAccountFailure : DoctorProfileEvent()

}

sealed class DoctorProfileEffect {
    object Logout : DoctorProfileEffect()
    object AskToConfirmLogout : DoctorProfileEffect()
    object LoadAccount : DoctorProfileEffect()

    data class UpdateAccount(
        val account: AccountDataModel
    ) : DoctorProfileEffect()

    object ShowUnexpectedError : DoctorProfileEffect()
}

@Parcelize
data class DoctorProfileDataModel(
    val account: AccountDataModel?,
    val isLoading: Boolean,
) : Parcelable

@Parcelize
data class AccountDataModel(
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val contactEmail: String?,
    val address: String?,
    val country: String?,
    val city: String?,
    val postalCode: String?,
    val measurement: String?
) : Parcelable

data class DoctorProfileViewModel(
    val fields: ProfileFieldListViewModel?,
    val isLoaderVisible: Boolean,
    val isContentVisible: Boolean,
    val maySave: Boolean,
)

data class ProfileFieldListViewModel(
    val fullNameField: ProfileFieldViewModel,
    val emailField: ProfileFieldViewModel,
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

fun DoctorProfileDataModel.viewModel(
    resourceProvider: ResourceProvider,
) = DoctorProfileViewModel(
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
            measurement = ProfileFieldViewModel(
                value = it.measurement,
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

private val DoctorAccount.dataModel: AccountDataModel
    get() = AccountDataModel(
        fullName = fullName,
        email = email,
        phoneNumber = phoneNumber,
        contactEmail = contactEmail,
        address = address,
        country = country,
        city = city,
        postalCode = postalCode,
        measurement = measurement
    )

private val AccountDataModel.model: DoctorAccount
    get() = DoctorAccount(
        fullName = fullName,
        email = email,
        phoneNumber = phoneNumber,
        contactEmail = contactEmail,
        address = address,
        country = country,
        city = city,
        postalCode = postalCode,
        measurement = measurement
    )