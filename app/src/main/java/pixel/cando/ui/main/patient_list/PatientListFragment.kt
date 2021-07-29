package pixel.cando.ui.main.patient_list

import android.graphics.Color
import android.os.Bundle
import pixel.cando.databinding.FragmentPatientListBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment

class PatientListFragment : ViewBindingFragment<FragmentPatientListBinding>() {

    override val viewBindingCreator: ViewBindingCreator<FragmentPatientListBinding>
        get() = FragmentPatientListBinding::inflate

    override fun onViewBindingCreated(
        viewBinding: FragmentPatientListBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.root.setBackgroundColor(Color.BLUE)
    }

}