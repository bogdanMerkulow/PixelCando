package pixel.cando.ui.main.patient_info

import android.os.Bundle
import pixel.cando.databinding.FragmentPatientInfoBinding
import pixel.cando.databinding.ListItemHeaderBinding
import pixel.cando.databinding.ListItemInfoPortionBinding
import pixel.cando.databinding.ListItemNumericMeasurementValueBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.list.createDifferAdapter
import pixel.cando.ui._base.list.createDifferAdapterDelegate
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.into
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.intoListDifferAdapter
import pixel.cando.utils.diffuser.intoSwipeRefresh
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.setListRoundedBg

class PatientInfoFragment : ViewBindingFragment<FragmentPatientInfoBinding>(
    FragmentPatientInfoBinding::inflate
), ViewModelRender<PatientInfoViewModel>,
    EventSenderNeeder<PatientInfoEvent>,
    DiffuserCreator<PatientInfoViewModel, FragmentPatientInfoBinding>,
    DiffuserProviderNeeder<PatientInfoViewModel> {

    override var eventSender: EventSender<PatientInfoEvent>? = null

    override var diffuserProvider: DiffuserProvider<PatientInfoViewModel>? = null

    private val adapter = createDifferAdapter(
        headerAdapterDelegate(),
        infoPortionAdapterDelegate(),
        measurementAdapterDelegate(),
    )

    override fun createDiffuser(
        viewBinding: FragmentPatientInfoBinding
    ): Diffuser<PatientInfoViewModel> {
        return Diffuser(
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(viewBinding.progressBar)
            ),
            map(
                { it.isRefreshing },
                intoSwipeRefresh(viewBinding.swipeRefresh)
            ),
            map(
                { it.listItems },
                intoListDifferAdapter(adapter)
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
        viewBinding: FragmentPatientInfoBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.list.setHasFixedSize(true)
        viewBinding.list.adapter = adapter
        viewBinding.swipeRefresh.setOnRefreshListener {
            eventSender?.sendEvent(
                PatientInfoEvent.RefreshRequest
            )
        }
        viewBinding.toolbar.setNavigationOnClickListener {
            eventSender?.sendEvent(
                PatientInfoEvent.ExitTap
            )
        }
    }

    override fun renderViewModel(viewModel: PatientInfoViewModel) {
        diffuserProvider?.invoke()?.run(viewModel)
    }
}

private fun headerAdapterDelegate(
) = createDifferAdapterDelegate<
        PatientInfoListItem.Header,
        PatientInfoListItem,
        ListItemHeaderBinding>(
    viewBindingCreator = ListItemHeaderBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.titleLabel.text = item.title
            binding.root.setListRoundedBg(
                isFirst = item.isFirst,
                isLast = item.isLast,
            )
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem == newItem
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun infoPortionAdapterDelegate(
) = createDifferAdapterDelegate<
        PatientInfoListItem.InfoPortion,
        PatientInfoListItem,
        ListItemInfoPortionBinding>(
    viewBindingCreator = ListItemInfoPortionBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.titleLabel.text = item.title
            binding.valueLabel.text = item.value
            binding.root.setListRoundedBg(
                isFirst = item.isFirst,
                isLast = item.isLast,
            )
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem == newItem
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun measurementAdapterDelegate(
) = createDifferAdapterDelegate<
        PatientInfoListItem.Measurement,
        PatientInfoListItem,
        ListItemNumericMeasurementValueBinding>(
    viewBindingCreator = ListItemNumericMeasurementValueBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.titleLabel.text = item.title
            binding.valueLabel.text = item.value
            binding.unitLabel.text = item.unit
            binding.root.setListRoundedBg(
                isFirst = item.isFirst,
                isLast = item.isLast,
            )
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem == newItem
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

