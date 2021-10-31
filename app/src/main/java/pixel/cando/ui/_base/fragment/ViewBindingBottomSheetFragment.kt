package pixel.cando.ui._base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


abstract class ViewBindingBottomSheetFragment<VB : ViewBinding> : BottomSheetDialogFragment() {

    protected abstract val viewBindingCreator: ViewBindingCreator<VB>

    private val viewBindingStore by lazy {
        ViewBindingStore(viewBindingCreator)
    }

    protected val viewBinding: VB?
        get() = viewBindingStore.viewBinding

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