package pixel.cando.ui.main.photo_preview

import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.FragmentManager
import pixel.cando.R
import pixel.cando.databinding.FragmentPhotoPreviewBinding
import pixel.cando.ui._base.fragment.ViewBindingFullscreenDialogFragment
import pixel.cando.ui._base.tea.ResultEmitter

class PhotoPreviewFragment : ViewBindingFullscreenDialogFragment<FragmentPhotoPreviewBinding>(
    FragmentPhotoPreviewBinding::inflate
) {

    companion object {
        fun show(
            photo: Bitmap,
            fragmentManager: FragmentManager
        ) {
            PhotoPreviewFragment().apply {
                this.photo = photo
                show(fragmentManager, "")
            }
        }
    }

    var photo: Bitmap? = null

    var resultEmitter: ResultEmitter<PhotoPreviewResult>? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentPhotoPreviewBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        val photo = this.photo
        if (photo == null) {
            dismiss()
            return
        }
        viewBinding.photoImageView.setImageBitmap(photo)
        viewBinding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        viewBinding.toolbar.menu.add("").apply {
            icon = getDrawable(requireContext(), R.drawable.ic_check_white)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener {
                dismiss()
                resultEmitter?.emit(
                    PhotoPreviewResult.Accepted(
                        bitmap = photo,
                    )
                )
                true
            }
        }
        viewBinding.toolbar.menu.add("").apply {
            icon = getDrawable(requireContext(), R.drawable.ic_cross_white)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            setOnMenuItemClickListener {
                dismiss()
                resultEmitter?.emit(
                    PhotoPreviewResult.Declined
                )
                true
            }
        }
    }

}

sealed class PhotoPreviewResult {
    data class Accepted(
        val bitmap: Bitmap,
    ) : PhotoPreviewResult()

    object Declined : PhotoPreviewResult()
}