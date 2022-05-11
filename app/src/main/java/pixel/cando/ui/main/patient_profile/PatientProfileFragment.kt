package pixel.cando.ui.main.patient_profile

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.Spinner
import com.google.android.material.textfield.TextInputLayout
import pixel.cando.R
import pixel.cando.databinding.FragmentPatientProfileBinding
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

class PatientProfileFragment : ViewBindingFragment<FragmentPatientProfileBinding>(
    FragmentPatientProfileBinding::inflate
), ViewModelRender<PatientProfileViewModel>,
    EventSenderNeeder<PatientProfileEvent>,
    DiffuserCreator<PatientProfileViewModel, FragmentPatientProfileBinding>,
    DiffuserProviderNeeder<PatientProfileViewModel> {

    override var eventSender: EventSender<PatientProfileEvent>? = null

    override var diffuserProvider: DiffuserProvider<PatientProfileViewModel>? = null

    override fun createDiffuser(
        viewBinding: FragmentPatientProfileBinding
    ): Diffuser<PatientProfileViewModel> {
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
                            { it?.patientCodeField },
                            fieldDiffuser(viewBinding.patientCodeFieldParent)
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
        viewBinding: FragmentPatientProfileBinding,
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
                    PatientProfileEvent.LogoutTap
                )
                true
            }

        viewBinding.saveButton.setOnClickListener {
            eventSender?.sendEvent(
                PatientProfileEvent.MeasurementChanged(
                    viewBinding.measurement.selectedItem.toString().lowercase(Locale.getDefault())
                )
            )
            eventSender?.sendEvent(
                PatientProfileEvent.SaveTap
            )
        }

        viewBinding.fullNameField.doAfterTextChanged {
            eventSender?.sendEvent(
                PatientProfileEvent.FullNameChanged(it)
            )
        }
        viewBinding.emailField.doAfterTextChanged {
            eventSender?.sendEvent(
                PatientProfileEvent.EmailChanged(it)
            )
        }
        viewBinding.phoneNumberField.doAfterTextChanged {
            eventSender?.sendEvent(
                PatientProfileEvent.PhoneNumberChanged(it)
            )
        }
        viewBinding.contactEmailField.doAfterTextChanged {
            eventSender?.sendEvent(
                PatientProfileEvent.ContactEmailChanged(it)
            )
        }
        viewBinding.addressField.doAfterTextChanged {
            eventSender?.sendEvent(
                PatientProfileEvent.AddressChanged(it)
            )
        }

        viewBinding.countryField.isEnabled = false
        viewBinding.patientCodeField.isEnabled = false

        viewBinding.cityField.doAfterTextChanged {
            eventSender?.sendEvent(
                PatientProfileEvent.CityChanged(it)
            )
        }
        viewBinding.zipField.doAfterTextChanged {
            eventSender?.sendEvent(
                PatientProfileEvent.PostalCodeChanged(it)
            )
        }

    }

    override fun renderViewModel(
        viewModel: PatientProfileViewModel
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
                val measurement = spinner.context.resources.getStringArray(R.array.measurement)

                val value = it?.replaceFirstChar { it.uppercase() }
                spinner.setSelection(measurement.indexOf(value))
            }
        )
    )
)