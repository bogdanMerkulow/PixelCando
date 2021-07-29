package pixel.cando.ui.main.photo_list

import pixel.cando.databinding.FragmentPhotoListBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class PhotoListFragment : ViewBindingFragment<FragmentPhotoListBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentPhotoListBinding>
        get() = FragmentPhotoListBinding::inflate

}