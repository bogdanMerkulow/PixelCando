package pixel.cando.ui._base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

typealias ViewBindingCreator<VB> = (LayoutInflater, ViewGroup?, Boolean) -> VB

class ViewBindingStore<VB : ViewBinding>(
    private val viewBindingCreator: ViewBindingCreator<VB>
) {

    var viewBinding: VB? = null
        private set

    fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View = viewBindingCreator.invoke(
        inflater,
        container,
        false
    ).also {
        viewBinding = it
    }.root

    fun onDestroyView() {
        viewBinding = null
    }

}

abstract class ViewBindingFragment<VB : ViewBinding> : DelegatingFragment() {

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
    ) = viewBindingStore.onCreateView(
        inflater,
        container
    )

    override fun onDestroyView() {
        super.onDestroyView()
        viewBindingStore.onDestroyView()
    }

}