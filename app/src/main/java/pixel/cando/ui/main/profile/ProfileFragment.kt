package pixel.cando.ui.main.profile

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.textfield.TextInputLayout
import pixel.cando.R
import pixel.cando.databinding.FragmentProfileBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.into
import pixel.cando.utils.diffuser.Diffuser.intoAll
import pixel.cando.utils.diffuser.Diffuser.intoOnce
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoEnabled
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.doAfterTextChanged

class ProfileFragment : ViewBindingFragment<FragmentProfileBinding>(
    FragmentProfileBinding::inflate
), ViewModelRender<ProfileViewModel>,
    EventSenderNeeder<ProfileEvent>,
    DiffuserCreator<ProfileViewModel, FragmentProfileBinding>,
    DiffuserProviderNeeder<ProfileViewModel> {

    override var eventSender: EventSender<ProfileEvent>? = null

    override var diffuserProvider: DiffuserProvider<ProfileViewModel>? = null

    override fun createDiffuser(
        viewBinding: FragmentProfileBinding
    ): Diffuser<ProfileViewModel> {
        return Diffuser(
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(viewBinding.progressBar)
            ),
            map(
                { it.isContentVisible },
                intoVisibleOrGone(viewBinding.scrollView)
            ),
            map(
                { it.maySave },
                intoEnabled(viewBinding.saveButton)
            ),
            map(
                { it.fields },
                intoAll(
                    listOf(
                        map(
                            { it?.fullNameField },
                            fieldDiffuser(viewBinding.fullNameFieldParent)
                        ),
                        map(
                            { it?.emailField },
                            fieldDiffuser(viewBinding.emailFieldParent)
                        ),
                        map(
                            { it?.phoneNumberField },
                            fieldDiffuser(viewBinding.phoneNumberFieldParent)
                        ),
                        map(
                            { it?.contactEmailField },
                            fieldDiffuser(viewBinding.contactEmailFieldParent)
                        ),
                        map(
                            { it?.addressField },
                            fieldDiffuser(viewBinding.addressFieldParent)
                        ),
                        map(
                            { it?.countryField },
                            fieldDiffuser(viewBinding.countryFieldParent)
                        ),
                    )
                )
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentProfileBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(
            viewBinding,
            savedInstanceState
        )

        viewBinding.toolbar.menu
            .add(R.string.profile_btn_logout)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            .setOnMenuItemClickListener {
                eventSender?.sendEvent(
                    ProfileEvent.LogoutTap
                )
                true
            }

        viewBinding.saveButton.setOnClickListener {
            eventSender?.sendEvent(
                ProfileEvent.SaveTap
            )
        }

        viewBinding.fullNameField.doAfterTextChanged {
            eventSender?.sendEvent(
                ProfileEvent.FullNameChanged(it)
            )
        }
        viewBinding.emailField.doAfterTextChanged {
            eventSender?.sendEvent(
                ProfileEvent.EmailChanged(it)
            )
        }
        viewBinding.phoneNumberField.doAfterTextChanged {
            eventSender?.sendEvent(
                ProfileEvent.PhoneNumberChanged(it)
            )
        }
        viewBinding.contactEmailField.doAfterTextChanged {
            eventSender?.sendEvent(
                ProfileEvent.ContactEmailChanged(it)
            )
        }
        viewBinding.addressField.doAfterTextChanged {
            eventSender?.sendEvent(
                ProfileEvent.AddressChanged(it)
            )
        }
        viewBinding.countryField.isEnabled = false

    }

    override fun renderViewModel(
        viewModel: ProfileViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }
}

private fun fieldDiffuser(
    textInputLayout: TextInputLayout
): Diffuser<ProfileFieldViewModel?> = intoAll(
    listOf(
        map(
            { it?.value },
            intoOnce {
                textInputLayout.editText?.setText(it)
            }
        ),
        map(
            { it?.error },
            into { textInputLayout.error = it }
        )
    )
)