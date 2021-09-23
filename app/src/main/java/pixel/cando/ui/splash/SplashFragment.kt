package pixel.cando.ui.splash

import pixel.cando.databinding.FragmentSplashBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class SplashFragment : ViewBindingFragment<FragmentSplashBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentSplashBinding>
        get() = FragmentSplashBinding::inflate

}