package pixel.cando.ui.main.patient_photo_review

import android.os.Bundle
import android.os.Parcelable
import coil.load
import coil.request.Disposable
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.databinding.FragmentPatientPhotoReviewBinding
import pixel.cando.ui._base.fragment.ViewBindingFullscreenDialogFragment
import pixel.cando.ui._base.fragment.findImplementation
import pixel.cando.utils.doAfterTextChanged
import pixel.cando.utils.gone
import pixel.cando.utils.visible

@Parcelize
data class PatientPhotoReviewArguments(
    val patientFullName: String,
    val photoUrl: String,
) : Parcelable

class PatientPhotoReviewFragment :
    ViewBindingFullscreenDialogFragment<FragmentPatientPhotoReviewBinding>(
        FragmentPatientPhotoReviewBinding::inflate
    ) {

    companion object {
        private const val KEY_ARG = "KEY_ARG"
        fun newInstance(
            arguments: PatientPhotoReviewArguments
        ): PatientPhotoReviewFragment {
            val args = Bundle()
            args.putParcelable(KEY_ARG, arguments)
            val fragment = PatientPhotoReviewFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var imageLoadDisposable: Disposable? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentPatientPhotoReviewBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        val args = requireArguments().getParcelable<PatientPhotoReviewArguments>(KEY_ARG)!!
        viewBinding.toolbar.title = args.patientFullName
        viewBinding.photoImageView.load(args.photoUrl)
        imageLoadDisposable = viewBinding.photoImageView.load(
            args.photoUrl
        ) {
            listener(
                onStart = {
                    viewBinding.progressBar.visible()
                },
                onCancel = {
                    viewBinding.progressBar.gone()
                },
                onError = { _, _ ->
                    viewBinding.progressBar.gone()
                },
                onSuccess = { _, _ ->
                    viewBinding.progressBar.gone()
                }
            )
        }
        viewBinding.rejectButton.isEnabled = false
        viewBinding.dialogGroup.gone()

        viewBinding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_accept -> {
                    dismiss()
                    findImplementation<Listener>()?.onAcceptPatientPhoto()
                }
                R.id.action_reject -> viewBinding.dialogGroup.visible()
            }
            true
        }
        viewBinding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        viewBinding.cancelButton.setOnClickListener {
            viewBinding.dialogGroup.gone()
        }
        viewBinding.rejectButton.setOnClickListener {
            dismiss()
            findImplementation<Listener>()?.onRejectPatientPhoto(
                viewBinding.reasonField.text?.toString().orEmpty()
            )
        }
        viewBinding.reasonField.doAfterTextChanged {
            viewBinding.rejectButton.isEnabled = it.isNotBlank()
        }
    }

    override fun onViewBindingDestroyed() {
        super.onViewBindingDestroyed()
        imageLoadDisposable?.dispose()
        imageLoadDisposable = null
    }

    interface Listener {
        fun onAcceptPatientPhoto()
        fun onRejectPatientPhoto(
            reason: String
        )
    }

}