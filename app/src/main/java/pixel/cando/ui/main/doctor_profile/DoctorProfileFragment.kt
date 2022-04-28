package pixel.cando.ui.main.doctor_profile

import android.os.Bundle
import android.view.MenuItem
import android.widget.Spinner
import com.google.android.material.textfield.TextInputLayout
import pixel.cando.R
import pixel.cando.databinding.FragmentDoctorProfileBinding
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
import java.util.*

class DoctorProfileFragment : ViewBindingFragment<FragmentDoctorProfileBinding>(
    FragmentDoctorProfileBinding::inflate
), ViewModelRender<DoctorProfileViewModel>,
    EventSenderNeeder<DoctorProfileEvent>,
    DiffuserCreator<DoctorProfileViewModel, FragmentDoctorProfileBinding>,
    DiffuserProviderNeeder<DoctorProfileViewModel> {

    override var eventSender: EventSender<DoctorProfileEvent>? = null

    override var diffuserProvider: DiffuserProvider<DoctorProfileViewModel>? = null

    override fun createDiffuser(
        viewBinding: FragmentDoctorProfileBinding
    ): Diffuser<DoctorProfileViewModel> {
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
                            { it?.measurement },
                            spinnerDiffuser(viewBinding.measurement)
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
                        map(
                            { it?.cityField },
                            fieldDiffuser(viewBinding.cityFieldParent)
                        ),
                        map(
                            { it?.postalCodeField },
                            fieldDiffuser(viewBinding.zipFieldParent)
                        ),
                    )
                )
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentDoctorProfileBinding,
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
                    DoctorProfileEvent.LogoutTap
                )
                true
            }

        viewBinding.saveButton.setOnClickListener {
            eventSender?.sendEvent(
                DoctorProfileEvent.MeasurementChanged(
                    viewBinding.measurement.selectedItem.toString().lowercase(Locale.getDefault())
                )
            )
            eventSender?.sendEvent(
                DoctorProfileEvent.SaveTap
            )
        }

        viewBinding.fullNameField.doAfterTextChanged {
            eventSender?.sendEvent(
                DoctorProfileEvent.FullNameChanged(it)
            )
        }
        viewBinding.emailField.doAfterTextChanged {
            eventSender?.sendEvent(
                DoctorProfileEvent.EmailChanged(it)
            )
        }
        viewBinding.phoneNumberField.doAfterTextChanged {
            eventSender?.sendEvent(
                DoctorProfileEvent.PhoneNumberChanged(it)
            )
        }
        viewBinding.contactEmailField.doAfterTextChanged {
            eventSender?.sendEvent(
                DoctorProfileEvent.ContactEmailChanged(it)
            )
        }
        viewBinding.addressField.doAfterTextChanged {
            eventSender?.sendEvent(
                DoctorProfileEvent.AddressChanged(it)
            )
        }
        viewBinding.countryField.isEnabled = false
        viewBinding.cityField.doAfterTextChanged {
            eventSender?.sendEvent(
                DoctorProfileEvent.CityChanged(it)
            )
        }
        viewBinding.zipField.doAfterTextChanged {
            eventSender?.sendEvent(
                DoctorProfileEvent.PostalCodeChanged(it)
            )
        }

    }

    override fun renderViewModel(
        viewModel: DoctorProfileViewModel
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

private fun spinnerDiffuser(
    spinner: Spinner
): Diffuser<ProfileFieldViewModel?> = intoAll(
    listOf(
        map(
            { it?.value },
            intoOnce {
                val selectedItemIndex = when(it?.lowercase(Locale.getDefault())) {
                    "metric" -> 0
                    "imperial" -> 1
                    else -> 0
                }
                spinner.setSelection(selectedItemIndex)
            }
        ),
        map(
            { it?.error },
            into {  }
        )
    )
)