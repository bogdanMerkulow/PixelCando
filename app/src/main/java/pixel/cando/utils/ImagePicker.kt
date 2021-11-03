package pixel.cando.utils

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import pixel.cando.ui._base.fragment.SimpleFragmentDelegate
import pixel.cando.ui._base.tea.ResultEmitter
import pixel.cando.ui._base.tea.ResultEventSource

interface ImagePicker {
    fun pickImage()
}

class RealImagePicker(
    private val resultEmitter: ResultEmitter<ImagePickerResult>
) : SimpleFragmentDelegate(),
    ImagePicker {

    private var launcher: ActivityResultLauncher<String>? = null

    override fun pickImage() {
        launcher?.launch("image/*")
    }

    override fun onFragmentCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    ) {
        launcher = fragment.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            if (it != null) {
                resultEmitter.emit(
                    ImagePickerResult(it)
                )
            }
        }
    }

}

data class ImagePickerResult(
    val uri: Uri
)

fun <E> createImagePickerResultEventSource(
    mapper: (ImagePickerResult) -> E
) = ResultEventSource(mapper)