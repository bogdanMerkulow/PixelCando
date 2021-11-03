package pixel.cando.ui.main.photo_preview

import android.os.Bundle
import pixel.cando.databinding.FragmentPhotoPreviewBinding
import pixel.cando.ui._base.fragment.OnBackPressedListener
import pixel.cando.ui._base.fragment.ViewBindingFullscreenDialogFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.intoOnce
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.doAfterTextChanged

class PhotoPreviewFragment : ViewBindingFullscreenDialogFragment<FragmentPhotoPreviewBinding>(
    FragmentPhotoPreviewBinding::inflate
), ViewModelRender<PhotoPreviewViewModel>,
    EventSenderNeeder<PhotoPreviewEvent>,
    DiffuserCreator<PhotoPreviewViewModel, FragmentPhotoPreviewBinding>,
    DiffuserProviderNeeder<PhotoPreviewViewModel>,
    OnBackPressedListener {

    override var eventSender: EventSender<PhotoPreviewEvent>? = null

    override var diffuserProvider: DiffuserProvider<PhotoPreviewViewModel>? = null

    override fun createDiffuser(
        viewBinding: FragmentPhotoPreviewBinding
    ): Diffuser<PhotoPreviewViewModel> {
        return Diffuser(
            map(
                { it.uri },
                intoOnce {
                    viewBinding.photoImageView.setImageURI(it)
                }
            ),
            map(
                { it.weight },
                intoOnce {
                    viewBinding.weightField.setText(it)
                }
            ),
            map(
                { it.height },
                intoOnce {
                    viewBinding.heightField.setText(it)
                }
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPhotoPreviewBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.toolbar.setNavigationOnClickListener {
            eventSender?.sendEvent(
                PhotoPreviewEvent.BackTap
            )
        }
        viewBinding.confirmButton.setOnClickListener {
            eventSender?.sendEvent(
                PhotoPreviewEvent.ConfirmTap
            )
        }
        viewBinding.cancelButton.setOnClickListener {
            eventSender?.sendEvent(
                PhotoPreviewEvent.CancelTap
            )
        }
        viewBinding.weightField.doAfterTextChanged {
            eventSender?.sendEvent(
                PhotoPreviewEvent.WeightChanged(it)
            )
        }
        viewBinding.heightField.isEnabled = false
    }

    override fun renderViewModel(
        viewModel: PhotoPreviewViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onBackPressed() {
        eventSender?.sendEvent(
            PhotoPreviewEvent.BackTap
        )
    }
}