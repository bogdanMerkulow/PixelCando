package pixel.cando.utils.diffuser

import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
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

class DiffuserFragmentDelegate<T, VB : ViewBinding>(
    private val diffuserCreator: DiffuserCreator<T, VB>
) : SimpleViewBindingFragmentDelegate<VB>() {

    var diffuser: Diffuser<T>? = null
        private set

    override fun onFragmentViewBindingCreated(
        fragment: Fragment,
        viewBinding: VB
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