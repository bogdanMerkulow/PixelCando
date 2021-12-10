package pixel.cando.ui.main.chat_with_doctor

import android.os.Bundle
import androidx.fragment.app.Fragment
import pixel.cando.databinding.FragmentChatWithDoctorBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment

class ChatWithDoctorFragment : ViewBindingFragment<FragmentChatWithDoctorBinding>(
    FragmentChatWithDoctorBinding::inflate
) {

    var contentFragmentProvider: (() -> Fragment)? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentChatWithDoctorBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
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

}