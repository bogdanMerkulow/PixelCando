package pixel.cando.ui.main.profile

import android.graphics.Color
import android.os.Bundle
import pixel.cando.databinding.FragmentProfileBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class ProfileFragment : ViewBindingFragment<FragmentProfileBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentProfileBinding>
        get() = FragmentProfileBinding::inflate

    override fun onViewBindingCreated(
        viewBinding: FragmentProfileBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.root.setBackgroundColor(Color.GREEN)
    }

}