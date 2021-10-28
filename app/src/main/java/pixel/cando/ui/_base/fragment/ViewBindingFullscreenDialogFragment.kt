package pixel.cando.ui._base.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.viewbinding.ViewBinding
import javax.annotation.OverridingMethodsMustInvokeSuper


abstract class ViewBindingFullscreenDialogFragment<VB : ViewBinding>(
    private val viewBindingCreator: ViewBindingCreator<VB>
) : DelegatingDialogFragment() {

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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
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