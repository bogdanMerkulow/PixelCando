package pixel.cando.ui.main.chat_with_patient

import android.os.Bundle
import androidx.fragment.app.Fragment
import pixel.cando.databinding.FragmentChatWithPatientBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender

class ChatWithPatientFragment : ViewBindingFragment<FragmentChatWithPatientBinding>(
    FragmentChatWithPatientBinding::inflate
), EventSenderNeeder<ChatWithPatientEvent>,
    ViewModelRender<ChatWithPatientViewModel> {

    override var eventSender: EventSender<ChatWithPatientEvent>? = null

    var contentFragmentProvider: (() -> Fragment)? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentChatWithPatientBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.toolbar.setNavigationOnClickListener {
            eventSender?.sendEvent(
                ChatWithPatientEvent.TapExit
            )
        }
        if (savedInstanceState == null
            && childFragmentManager.fragments.isEmpty()
        ) {
            contentFragmentProvider?.invoke()?.let {
                childFragmentManager.beginTransaction()
                    .add(
                        viewBinding.container.id,
                        it
                    )
                    .commit()
            }
        }
    }

    override fun renderViewModel(
        viewModel: ChatWithPatientViewModel
    ) {
        viewBinding?.apply {
            toolbar.title = viewModel.title
        }
    }
}