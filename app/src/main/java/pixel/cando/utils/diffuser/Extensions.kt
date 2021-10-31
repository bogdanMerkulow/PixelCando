package pixel.cando.utils.diffuser

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import pixel.cando.ui._base.fragment.SimpleViewBindingFragmentDelegate

fun <A, B> map(
    transform: (A) -> B,
    diffuser: Diffuser<B>
): Diffuser<A> {
    return Diffuser.map(
        { transform.invoke(it) },
        diffuser
    )
}

fun <T> intoListDifferAdapter(
    listAdapter: AsyncListDifferDelegationAdapter<T>
): Diffuser<List<T>?> {
    return Diffuser.intoAlways { list: List<T>? ->
        listAdapter.items = list
    }
}

fun intoSwipeRefresh(
    swipeRefresh: SwipeRefreshLayout
): Diffuser<Boolean> {
    return Diffuser.intoAlways {
        swipeRefresh.isRefreshing = it
    }
}

class DiffuserFragmentDelegate<T, VB : ViewBinding>(
    private val diffuserCreator: DiffuserCreator<T, VB>
) : SimpleViewBindingFragmentDelegate<VB>() {

    var diffuser: Diffuser<T>? = null
        private set

    override fun onFragmentViewBindingCreated(
        fragment: Fragment,
        viewBinding: VB,
        savedInstanceState: Bundle?
    ) {
        diffuser = diffuserCreator.createDiffuser(viewBinding)
    }

    override fun onFragmentViewBindingDestroyed(
        fragment: Fragment
    ) {
        diffuser = null
    }

}

interface DiffuserCreator<T, VB : ViewBinding> {

    fun createDiffuser(
        viewBinding: VB
    ): Diffuser<T>

}

typealias DiffuserProvider<T> = () -> Diffuser<T>?

interface DiffuserProviderNeeder<T> {

    var diffuserProvider: DiffuserProvider<T>?

}