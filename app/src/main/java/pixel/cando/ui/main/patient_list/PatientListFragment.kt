package pixel.cando.ui.main.patient_list

import android.os.Bundle
import pixel.cando.R
import pixel.cando.databinding.FragmentPatientListBinding
import pixel.cando.databinding.ListItemPatientBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.list.createDifferAdapter
import pixel.cando.ui._base.list.createDifferAdapterDelegate
import pixel.cando.ui._base.list.isRefreshing
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.ui._commmon.*
import pixel.cando.utils.addLoadMoreListener
import pixel.cando.utils.context
import pixel.cando.utils.diffuser.*
import pixel.cando.utils.diffuser.Diffuser.into

class PatientListFragment : ViewBindingFragment<FragmentPatientListBinding>(),
    ViewModelRender<PatientListViewModel>,
    EventSenderNeeder<PatientListEvent>,
    DiffuserCreator<PatientListViewModel, FragmentPatientListBinding>,
    DiffuserProviderNeeder<PatientListViewModel> {

    override val viewBindingCreator: ViewBindingCreator<FragmentPatientListBinding>
        get() = FragmentPatientListBinding::inflate

    override var eventSender: EventSender<PatientListEvent>? = null

    override var diffuserProvider: DiffuserProvider<PatientListViewModel>? = null

    private val adapter by lazy {
        createDifferAdapter(
            patientAdapterDelegate {
                eventSender?.sendEvent(
                    PatientListEvent.PickPatient(it)
                )
            },
            noDataListPlaceholder<PatientListItem.NoDataPlaceholder, PatientListItem>(),
            listInitialLoader<PatientListItem.InitialLoader, PatientListItem>(),
            listMoreLoader<PatientListItem.MoreLoader, PatientListItem>(),
        )
    }

    override fun createDiffuser(
        viewBinding: FragmentPatientListBinding
    ): Diffuser<PatientListViewModel> {
        return Diffuser(
            map(
                { it.listState.isRefreshing },
                into { viewBinding.swipeRefresh.isRefreshing = it }
            ),
            map(
                {
                    it.listState.toListItems(
                        noDataPlaceholderProvider = {
                            PatientListItem.NoDataPlaceholder(
                                viewBinding.context.getString(R.string.no_patients)
                            )
                        },
                        initialLoaderProvider = {
                            PatientListItem.InitialLoader
                        },
                        moreLoaderProvider = {
                            PatientListItem.MoreLoader
                        },
                        itemMapper = {
                            PatientListItem.Patient(it)
                        }
                    )
                },
                intoListDifferAdapter(adapter)
            ),
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPatientListBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.list.adapter = adapter
        viewBinding.swipeRefresh.setOnRefreshListener {
            eventSender?.sendEvent(
                PatientListEvent.RefreshRequest
            )
        }
        viewBinding.list.addLoadMoreListener {
            eventSender?.sendEvent(
                PatientListEvent.LoadNextPage
            )
        }
    }

    override fun renderViewModel(
        viewModel: PatientListViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }
}

private sealed class PatientListItem : ListItem {
    object InitialLoader : PatientListItem(),
        ListInitialLoader

    object MoreLoader : PatientListItem(),
        ListMoreLoader

    data class NoDataPlaceholder(
        override val text: String
    ) : PatientListItem(),
        NoDataListPlaceholder

    data class Patient(
        val patient: PatientViewModel
    ) : PatientListItem()
}

private fun patientAdapterDelegate(
    clickListener: (Long) -> Unit
) = createDifferAdapterDelegate<
        PatientListItem.Patient,
        PatientListItem,
        ListItemPatientBinding>(
    viewBindingCreator = ListItemPatientBinding::inflate,
    viewHolderBinding = {
        binding.root.setOnClickListener {
            clickListener.invoke(item.patient.id)
        }
        bind {
            binding.title.text = item.patient.fullName
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem.patient.id == newItem.patient.id
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)