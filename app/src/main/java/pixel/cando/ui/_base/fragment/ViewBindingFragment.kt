package pixel.cando.ui._base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import javax.annotation.OverridingMethodsMustInvokeSuper

typealias ViewBindingCreator<VB> = (LayoutInflater, ViewGroup?, Boolean) -> VB

class ViewBindingStore<VB : ViewBinding>(
    private val viewBindingCreator: ViewBindingCreator<VB>
) {

    var viewBinding: VB? = null
        private set

    fun createViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View = viewBindingCreator.invoke(
        inflater,
        container,
        false
    ).also {
        viewBinding = it
    }.root

    fun destroyViewBinding() {
        viewBinding = null
    }

}

abstract class ViewBindingFragment<VB : ViewBinding>(
    private val viewBindingCreator: ViewBindingCreator<VB>
) : DelegatingFragment() {

    private val viewBindingStore by lazy(LazyThreadSafetyMode.NONE) {
        ViewBindingStore(viewBindingCreator)
    }

    protected val viewBinding: VB?
        get() = viewBindingStore.viewBinding

    @OverridingMethodsMustInvokeSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = viewBindingStore.createViewBinding(
        inflater,
        container
    )

    @OverridingMethodsMustInvokeSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        viewBinding?.let {
            onViewBindingCreated(
                it,
                savedInstanceState
            )
        }
        super.onViewCreated(view, savedInstanceState)
    }

    @OverridingMethodsMustInvokeSuper
    open fun onViewBindingCreated(
        viewBinding: VB,
        savedInstanceState: Bundle?
    ) {
        delegates
            .mapNotNull {
                it as? ViewBindingFragmentDelegate<VB>
            }
            .forEach {
                it.onFragmentViewBindingCreated(
                    this,
                    viewBinding,
                    savedInstanceState
                )
            }
    }

    @OverridingMethodsMustInvokeSuper
    override fun onDestroyView() {
        super.onDestroyView()
        viewBindingStore.destroyViewBinding()
        onViewBindingDestroyed()
    }

    @OverridingMethodsMustInvokeSuper
    open fun onViewBindingDestroyed() {
        delegates
            .mapNotNull {
                it as? ViewBindingFragmentDelegate<VB>
            }
            .forEach {
                it.onFragmentViewBindingDestroyed(
                    this
                )
            }
    }

}

interface ViewBindingFragmentDelegate<VB : ViewBinding> : FragmentDelegate {

    fun onFragmentViewBindingCreated(
        fragment: Fragment,
        viewBinding: VB,
        savedInstanceState: Bundle?
    )

    fun onFragmentViewBindingDestroyed(
        fragment: Fragment
    )

}

open class SimpleViewBindingFragmentDelegate<VB : ViewBinding> : SimpleFragmentDelegate(),
    ViewBindingFragmentDelegate<VB> {

    override fun onFragmentViewBindingCreated(
        fragment: Fragment,
        viewBinding: VB,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onFragmentViewBindingDestroyed(
        fragment: Fragment
    ) {
    }
}
