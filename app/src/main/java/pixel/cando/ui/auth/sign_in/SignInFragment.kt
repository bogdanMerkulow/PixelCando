package pixel.cando.ui.auth.sign_in

import android.net.Uri
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pixel.cando.R
import pixel.cando.databinding.FragmentSignInBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.intoOnce
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoEnabled
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.doAfterTextChanged
import pixel.cando.utils.hideKeyboard

class SignInFragment : ViewBindingFragment<FragmentSignInBinding>(
    FragmentSignInBinding::inflate
), ViewModelRender<SignInViewModel>,
    EventSenderNeeder<SignInEvent>,
    DiffuserCreator<SignInViewModel, FragmentSignInBinding>,
    DiffuserProviderNeeder<SignInViewModel>,
    CameraFragment.Callback {

    override var eventSender: EventSender<SignInEvent>? = null

    override var diffuserProvider: DiffuserProvider<SignInViewModel>? = null

    override fun createDiffuser(
        viewBinding: FragmentSignInBinding
    ): Diffuser<SignInViewModel> {
        return Diffuser(
            map(
                { it.email },
                intoOnce {
                    viewBinding.emailField.setText(it)
                }
            ),
            map(
                { it.password },
                intoOnce {
                    viewBinding.passwordField.setText(it)
                }
            ),
            map(
                { it.isSignInButtonEnabled },
                intoEnabled(viewBinding.signInButton)
            ),
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(viewBinding.progressBar)
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentSignInBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.emailField.doAfterTextChanged { email ->
            eventSender?.sendEvent(
                SignInEvent.EmailChanged(
                    email = email
                )
            )
        }
        viewBinding.passwordField.doAfterTextChanged { password ->
            eventSender?.sendEvent(
                SignInEvent.PasswordChanged(
                    password = password
                )
            )
        }
        viewBinding.signInButton.setOnClickListener {
            hideKeyboard()
            eventSender?.sendEvent(SignInEvent.TapSignIn)
        }
        viewBinding.takePhotoButton.setOnClickListener {
            eventSender?.sendEvent(SignInEvent.TapTakePhoto)
        }
        viewBinding.recoverPassword.setOnClickListener {
            eventSender?.sendEvent(SignInEvent.TapRecoverPassword)
        }
    }

    override fun renderViewModel(
        viewModel: SignInViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onCameraResult(uri: Uri) {
        eventSender?.sendEvent(
            SignInEvent.PhotoTaken(
                uri = uri
            )
        )
    }

    override fun onCameraCancel() {}

    fun showTakePhotoSuccessMessage() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sing_in_take_photo_success_title)
            .setMessage(R.string.sing_in_take_photo_success_message)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ -> }
            .create()
            .show()
    }

}