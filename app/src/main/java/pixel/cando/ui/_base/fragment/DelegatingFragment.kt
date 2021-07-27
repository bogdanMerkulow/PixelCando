package pixel.cando.ui._base.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

open class DelegatingFragment : Fragment() {

    var delegates: Set<FragmentDelegate> = setOf()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        delegates.forEach {
            it.onFragmentAttached(
                this,
                context
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegates.forEach {
            it.onFragmentCreated(
                this,
                savedInstanceState
            )
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )
        delegates.forEach {
            it.onFragmentViewCreated(
                this,
                savedInstanceState
            )
        }
    }

    override fun onStart() {
        super.onStart()
        delegates.forEach {
            it.onFragmentStarted(this)
        }
    }

    override fun onResume() {
        super.onResume()
        delegates.forEach {
            it.onFragmentResumed(this)
        }
    }

    override fun onPause() {
        super.onPause()
        delegates.forEach {
            it.onFragmentPaused(this)
        }
    }

    override fun onStop() {
        super.onStop()
        delegates.forEach {
            it.onFragmentStopped(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        delegates.forEach {
            it.onFragmentSaveInstanceState(
                this,
                outState
            )
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        delegates.forEach {
            it.onFragmentViewStateRestored(
                this,
                savedInstanceState
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        delegates.forEach {
            it.onFragmentViewDestroyed(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        delegates.forEach {
            it.onFragmentDestroyed(this)
        }
    }

    override fun onDetach() {
        super.onDetach()
        delegates.forEach {
            it.onFragmentDetached(this)
        }
    }

}


interface FragmentDelegate {

    fun onFragmentAttached(
        fragment: Fragment,
        context: Context
    )

    fun onFragmentCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    )

    fun onFragmentViewCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    )

    fun onFragmentStarted(
        fragment: Fragment
    )

    fun onFragmentResumed(
        fragment: Fragment
    )

    fun onFragmentPaused(
        fragment: Fragment
    )

    fun onFragmentStopped(
        fragment: Fragment
    )

    fun onFragmentHiddenChanged(hidden: Boolean)

    fun onFragmentSaveInstanceState(
        fragment: Fragment,
        outState: Bundle
    )

    fun onFragmentViewStateRestored(
        fragment: Fragment,
        savedInstanceState: Bundle?
    )

    fun onFragmentViewDestroyed(
        fragment: Fragment
    )

    fun onFragmentDestroyed(
        fragment: Fragment
    )

    fun onFragmentDetached(
        fragment: Fragment
    )
}

open class SimpleFragmentDelegate : FragmentDelegate {

    override fun onFragmentAttached(
        fragment: Fragment,
        context: Context
    ) {
    }

    override fun onFragmentCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onFragmentViewCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onFragmentStarted(fragment: Fragment) {
    }

    override fun onFragmentResumed(fragment: Fragment) {
    }

    override fun onFragmentPaused(fragment: Fragment) {
    }

    override fun onFragmentStopped(fragment: Fragment) {
    }

    override fun onFragmentHiddenChanged(hidden: Boolean) {
    }

    override fun onFragmentSaveInstanceState(
        fragment: Fragment,
        outState: Bundle
    ) {
    }

    override fun onFragmentViewStateRestored(
        fragment: Fragment,
        savedInstanceState: Bundle?
    ) {
    }

    override fun onFragmentViewDestroyed(fragment: Fragment) {
    }

    override fun onFragmentDestroyed(fragment: Fragment) {
    }

    override fun onFragmentDetached(fragment: Fragment) {
    }
}