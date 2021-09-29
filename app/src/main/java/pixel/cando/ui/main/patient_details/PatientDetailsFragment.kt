package pixel.cando.ui.main.patient_details

import android.graphics.Bitmap
import android.os.Bundle
import pixel.cando.databinding.FragmentPatientDetailsBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.utils.diffuser.*
import pixel.cando.utils.diffuser.ViewDiffusers.intoEnabled
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone

class PatientDetailsFragment : ViewBindingFragment<FragmentPatientDetailsBinding>(
    FragmentPatientDetailsBinding::inflate
), ViewModelRender<PatientDetailsViewModel>,
    EventSenderNeeder<PatientDetailsEvent>,
    DiffuserCreator<PatientDetailsViewModel, FragmentPatientDetailsBinding>,
    DiffuserProviderNeeder<PatientDetailsViewModel>,
    CameraFragment.Callback {

    override var eventSender: EventSender<PatientDetailsEvent>? = null

    override var diffuserProvider: DiffuserProvider<PatientDetailsViewModel>? = null

    override fun createDiffuser(
        viewBinding: FragmentPatientDetailsBinding
    ): Diffuser<PatientDetailsViewModel> {
        return Diffuser(
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(
                    viewBinding.progressBar
                )
            ),
            map(
                { it.isTakePhotoButtonEnabled },
                intoEnabled(
                    viewBinding.takePhotoButton
                )
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPatientDetailsBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)

        viewBinding.toolbar.setNavigationOnClickListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.ExitTap
            )
        }

        viewBinding.takePhotoButton.setOnClickListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.TakePhotoTap
            )
        }
    }

    override fun renderViewModel(
        viewModel: PatientDetailsViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onCameraResult(
        bitmap: Bitmap
    ) {
        eventSender?.sendEvent(
            PatientDetailsEvent.PhotoTaken(
                bitmap = bitmap
            )
        )
    }

    override fun onCameraCancel() {
    }

}