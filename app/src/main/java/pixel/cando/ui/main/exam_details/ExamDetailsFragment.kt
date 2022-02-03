package pixel.cando.ui.main.exam_details

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import pixel.cando.databinding.FragmentExamDetailsBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.into
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.map

class ExamDetailsFragment : ViewBindingFragment<FragmentExamDetailsBinding>(
    FragmentExamDetailsBinding::inflate
), ViewModelRender<ExamDetailsViewModel>,
    EventSenderNeeder<ExamDetailsEvent>,
    DiffuserCreator<ExamDetailsViewModel, FragmentExamDetailsBinding>,
    DiffuserProviderNeeder<ExamDetailsViewModel> {

    override var eventSender: EventSender<ExamDetailsEvent>? = null

    override var diffuserProvider: DiffuserProvider<ExamDetailsViewModel>? = null

    private var pagerAdapter: ViewPagerAdapter? = null

    override fun createDiffuser(
        viewBinding: FragmentExamDetailsBinding
    ): Diffuser<ExamDetailsViewModel> {
        return Diffuser(
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(viewBinding.progressBar)
            ),
            map(
                { it.areTabsVisible },
                intoVisibleOrGone(viewBinding.tabLayout)
            ),
            map(
                { it.tabs },
                into {
                    pagerAdapter?.setItems(it)
                }
            ),
            map(
                { it.title },
                into {
                    viewBinding.toolbar.title = it
                }
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentExamDetailsBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.toolbar.setNavigationOnClickListener {
            eventSender?.sendEvent(
                ExamDetailsEvent.ExitTap
            )
        }
        val pagerAdapter = ViewPagerAdapter(this).also {
            pagerAdapter = it
        }
        viewBinding.viewPager.adapter = pagerAdapter
        TabLayoutMediator(
            viewBinding.tabLayout,
            viewBinding.viewPager
        ) { tab, position ->
            tab.text = pagerAdapter.getTitle(position)
        }.attach()
    }

    override fun onViewBindingDestroyed() {
        super.onViewBindingDestroyed()
    }

    override fun renderViewModel(
        viewModel: ExamDetailsViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

}

private class ViewPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    private val items = mutableListOf<ExamDetailsTabViewModel>()

    fun setItems(
        items: List<ExamDetailsTabViewModel>
    ) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun createFragment(
        position: Int
    ) = when (val item = items[position]) {
        is ExamDetailsTabViewModel.NumericMeasurementValues -> {
            ExamNumericMeasurementValuesFragment.newInstance(
                ExamNumericMeasurementValuesFragment.Arguments(
                    createdAt = item.createdAt,
                    weight = item.weight,
                    bmi = item.bmi,
                    bmr = item.bmr,
                    fm = item.fm,
                    ffm = item.ffm,
                    abdominalFatMass = item.abdominalFatMass,
                    tbw = item.tbw,
                    hip = item.hip,
                    belly = item.belly,
                    waistToHeight = item.waistToHeight,
                )
            )
        }
        is ExamDetailsTabViewModel.Silhouette -> {
            ExamSilhouetteFragment.newInstance(
                silhouetteUrl = item.silhouetteUrl
            )
        }
    }

    fun getTitle(
        position: Int
    ) = items[position].title

}