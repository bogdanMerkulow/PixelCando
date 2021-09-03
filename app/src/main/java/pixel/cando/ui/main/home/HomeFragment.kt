package pixel.cando.ui.main.home

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import pixel.cando.databinding.FragmentHomeBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder

class HomeFragment : ViewBindingFragment<FragmentHomeBinding>(),
    ViewModelRender<HomeViewModel>,
    EventSenderNeeder<HomeEvent>,
    DiffuserCreator<HomeViewModel, FragmentHomeBinding>,
    DiffuserProviderNeeder<HomeViewModel> {

    override val viewBindingCreator: ViewBindingCreator<FragmentHomeBinding>
        get() = FragmentHomeBinding::inflate

    var tabs: List<HomeTab> = emptyList()

    override var eventSender: EventSender<HomeEvent>? = null

    override var diffuserProvider: DiffuserProvider<HomeViewModel>? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentHomeBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(
            viewBinding,
            savedInstanceState
        )
        for (index in tabs.indices) {
            val tab = tabs[index]
            viewBinding.bottomNavigationView.menu
                .add(0, index, 0, tab.title)
                .setIcon(tab.icon)
                .setOnMenuItemClickListener {
                    eventSender?.sendEvent(
                        HomeEvent.SelectTab(index)
                    )
                    false
                }
        }
    }

    override fun createDiffuser(
        viewBinding: FragmentHomeBinding
    ): Diffuser<HomeViewModel> {
        return Diffuser.into {
            viewBinding.bottomNavigationView.apply {
                selectedItemId = menu.getItem(it.selectedIndex).itemId
            }
            openTabWithIndex(it.selectedIndex)
        }
    }

    override fun renderViewModel(
        viewModel: HomeViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    private fun openTabWithIndex(
        index: Int
    ) {
        val viewBinding = this.viewBinding
            ?: return
        if (index in tabs.indices) {
            val fragmentToAttach = childFragmentManager.findFragmentByTag(index.tag)
            if (fragmentToAttach != null) {
                childFragmentManager
                    .beginTransaction()
                    .apply {
                        childFragmentManager.fragments
                            .filter { it != fragmentToAttach }
                            .forEach {
                                detach(it)
                            }
                        attach(fragmentToAttach)
                    }
                    .commit()
            } else {
                val fragmentToAdd = tabs[index].fragmentProvider.invoke()
                childFragmentManager
                    .beginTransaction()
                    .apply {
                        childFragmentManager.fragments
                            .forEach {
                                detach(it)
                            }
                        add(
                            viewBinding.container.id,
                            fragmentToAdd,
                            index.tag
                        )
                    }
                    .commit()
            }
        }
    }

}

private val Int.tag: String
    get() = "${HomeFragment::class.java.name}_Tab_$this"

data class HomeTab(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val fragmentProvider: () -> Fragment,
)