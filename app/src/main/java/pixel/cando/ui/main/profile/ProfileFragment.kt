package pixel.cando.ui.main.profile

import pixel.cando.databinding.FragmentProfileBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class ProfileFragment : ViewBindingFragment<FragmentProfileBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentProfileBinding>
        get() = FragmentProfileBinding::inflate

}