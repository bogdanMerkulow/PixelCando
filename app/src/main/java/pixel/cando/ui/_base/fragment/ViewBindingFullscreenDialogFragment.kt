package pixel.cando.ui._base.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding


abstract class ViewBindingFullscreenDialogFragment<VB : ViewBinding> : DialogFragment() {

    protected abstract val viewBindingCreator: ViewBindingCreator<VB>

    private val viewBindingStore by lazy {
        ViewBindingStore(viewBindingCreator)
    }

    protected val viewBinding: VB?
        get() = viewBindingStore.viewBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
            .also {
                it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                it.window?.requestFeature(Window.FEATURE_NO_TITLE)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = viewBindingStore.createViewBinding(
        inflater,
        container
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        viewBinding?.let {
            onViewBindingCreated(it, savedInstanceState)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    open fun onViewBindingCreated(
        viewBinding: VB,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBindingStore.destroyViewBinding()
    }

}