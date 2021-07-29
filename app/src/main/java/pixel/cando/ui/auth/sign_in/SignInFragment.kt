package pixel.cando.ui.auth.sign_in

import android.os.Bundle
import pixel.cando.databinding.FragmentSignInBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.utils.diffuser.*
import pixel.cando.utils.diffuser.Diffuser.intoOnce
import pixel.cando.utils.diffuser.ViewDiffusers.intoEnabled
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.doAfterTextChanged

class SignInFragment : ViewBindingFragment<FragmentSignInBinding>(),
    ViewModelRender<SignInViewModel>,
    EventSenderNeeder<SignInEvent>,
    DiffuserCreator<SignInViewModel, FragmentSignInBinding>,
    DiffuserProviderNeeder<SignInViewModel> {

    override val viewBindingCreator: ViewBindingCreator<FragmentSignInBinding>
        get() = FragmentSignInBinding::inflate

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
            eventSender?.sendEvent(SignInEvent.TapSignIn)
        }
    }

    override fun renderViewModel(
        viewModel: SignInViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }
}