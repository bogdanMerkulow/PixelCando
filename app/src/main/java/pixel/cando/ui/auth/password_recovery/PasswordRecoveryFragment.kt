package pixel.cando.ui.auth.password_recovery

import android.os.Bundle
import pixel.cando.databinding.FragmentPasswordRecoveryBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
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

class PasswordRecoveryFragment : ViewBindingFragment<FragmentPasswordRecoveryBinding>(
    FragmentPasswordRecoveryBinding::inflate
), ViewModelRender<PasswordRecoveryViewModel>,
    EventSenderNeeder<PasswordRecoveryEvent>,
    DiffuserCreator<PasswordRecoveryViewModel, FragmentPasswordRecoveryBinding>,
    DiffuserProviderNeeder<PasswordRecoveryViewModel> {

    override var eventSender: EventSender<PasswordRecoveryEvent>? = null

    override var diffuserProvider: DiffuserProvider<PasswordRecoveryViewModel>? = null

    override fun createDiffuser(
        viewBinding: FragmentPasswordRecoveryBinding
    ): Diffuser<PasswordRecoveryViewModel> {
        return Diffuser(
            map(
                { it.email },
                intoOnce {
                    viewBinding.emailField.setText(it)
                }
            ),
            map(
                { it.isRecoveryButtonEnabled },
                intoEnabled(viewBinding.recoverPasswordButton)
            ),
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(viewBinding.progressBar)
            ),
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPasswordRecoveryBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.toolbar.setNavigationOnClickListener {
            eventSender?.sendEvent(
                PasswordRecoveryEvent.TapExit
            )
        }
        viewBinding.emailField.doAfterTextChanged {
            eventSender?.sendEvent(
                PasswordRecoveryEvent.EmailChanged(it)
            )
        }
        viewBinding.recoverPasswordButton.setOnClickListener {
            hideKeyboard()
            eventSender?.sendEvent(
                PasswordRecoveryEvent.TapRecover
            )
        }
    }

    override fun renderViewModel(
        viewModel: PasswordRecoveryViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }
}