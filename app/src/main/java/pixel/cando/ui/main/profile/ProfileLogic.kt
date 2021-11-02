package pixel.cando.ui.main.profile

import android.os.Parcelable
import android.util.Patterns
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.local.SessionWiper
import pixel.cando.data.models.Account
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.RootRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight

object ProfileLogic {

    fun init(
        model: ProfileDataModel
    ): First<ProfileDataModel, ProfileEffect> {
        if (model.account == null) {
            return First.first(
                model.copy(
                    isLoading = true,
                ),
                setOf(
                    ProfileEffect.LoadAccount
                )
            )
        }
        return First.first(model)
    }

    fun update(
        model: ProfileDataModel,
        event: ProfileEvent
    ): Next<ProfileDataModel, ProfileEffect> {
        return when (event) {
            // ui
            is ProfileEvent.FullNameChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            fullName = event.value
                        )
                    )
                )
            }
            is ProfileEvent.EmailChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            email = event.value
                        )
                    )
                )
            }
            is ProfileEvent.PhoneNumberChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            phoneNumber = event.value
                        )
                    )
                )
            }
            is ProfileEvent.ContactEmailChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            contactEmail = event.value
                        )
                    )
                )
            }
            is ProfileEvent.AddressChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            address = event.value
                        )
                    )
                )
            }
            is ProfileEvent.CountryChanged -> {
                Next.next(
                    model.copy(
                        account = model.account?.copy(
                            country = event.value
                        )
                    )
                )
            }
            is ProfileEvent.LogoutTap -> {
                Next.dispatch(
                    setOf(
                        ProfileEffect.Logout
                    )
                )
            }
            // model
            is ProfileEvent.AccountLoadSuccess -> {
                Next.next(
                    model.copy(
                        account = event.account,
                        isLoading = false,
                    )
                )
            }
            is ProfileEvent.AccountLoadFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        ProfileEffect.ShowUnexpectedError
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
    ): Connectable<ProfileEffect, ProfileEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is ProfileEffect.LoadAccount -> {
                    val result = remoteRepository.getAccount()
                    result.onLeft {
                        output.accept(
                            ProfileEvent.AccountLoadSuccess(
                                it.dataModel
                            )
                        )
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            ProfileEvent.AccountLoadFailure
                        )
                    }
                }
                is ProfileEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
                is ProfileEffect.Logout -> {
                    sessionWiper.wipe()
                    rootRouter.replaceScreen(
                        Screens.authFlow()
                    )
                }
            }
        }

    fun initialModel(
    ) = ProfileDataModel(
        account = null,
        isLoading = false,
    )

}

sealed class ProfileEvent {
    // ui
    data class FullNameChanged(
        val value: String
    ) : ProfileEvent()

    data class EmailChanged(
        val value: String
    ) : ProfileEvent()

    data class PhoneNumberChanged(
        val value: String
    ) : ProfileEvent()

    data class ContactEmailChanged(
        val value: String
    ) : ProfileEvent()

    data class AddressChanged(
        val value: String
    ) : ProfileEvent()

    data class CountryChanged(
        val value: String
    ) : ProfileEvent()

    object LogoutTap : ProfileEvent()

    // model
    data class AccountLoadSuccess(
        val account: AccountDataModel
    ) : ProfileEvent()

    object AccountLoadFailure : ProfileEvent()
}

sealed class ProfileEffect {
    object Logout : ProfileEffect()
    object LoadAccount : ProfileEffect()
    object ShowUnexpectedError : ProfileEffect()
}

@Parcelize
data class ProfileDataModel(
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
) : Parcelable

data class ProfileViewModel(
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
)

data class ProfileFieldViewModel(
    val value: String?,
    val error: String?
)

fun ProfileDataModel.viewModel(
    resourceProvider: ResourceProvider,
) = ProfileViewModel(
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

private val Account.dataModel: AccountDataModel
    get() = AccountDataModel(
        fullName = fullName,
        email = email,
        phoneNumber = phoneNumber,
        contactEmail = contactEmail,
        address = address,
        country = country,
    )