package pixel.cando.ui.main.profile

import android.os.Bundle
import pixel.cando.databinding.FragmentProfileBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder

class ProfileFragment : ViewBindingFragment<FragmentProfileBinding>(
    FragmentProfileBinding::inflate
), EventSenderNeeder<ProfileEvent> {

    override var eventSender: EventSender<ProfileEvent>? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentProfileBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(
            viewBinding,
            savedInstanceState
        )
        viewBinding.logoutButton.setOnClickListener {
            eventSender?.sendEvent(
                ProfileEvent.LogoutTap
            )
        }
    }

}