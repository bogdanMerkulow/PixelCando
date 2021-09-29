package pixel.cando.ui.main.patient_list

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.tabs.TabLayout
import pixel.cando.R
import pixel.cando.databinding.FragmentPatientListBinding
import pixel.cando.databinding.ListItemPatientBinding
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

class PatientListFragment : ViewBindingFragment<FragmentPatientListBinding>(
    FragmentPatientListBinding::inflate
), ViewModelRender<PatientListViewModel>,
    EventSenderNeeder<PatientListEvent>,
    DiffuserCreator<PatientListViewModel, FragmentPatientListBinding>,
    DiffuserProviderNeeder<PatientListViewModel> {

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
                { it.folders },
                into {
                    viewBinding.folderTabs.removeAllTabs()

                    viewBinding.folderTabs.addTab(
                        viewBinding.folderTabs.newTab().apply {
                            text = viewBinding.context.getString(R.string.all)
                        }
                    )

                    it.forEach { folder ->
                        val tab = viewBinding.folderTabs.newTab()
                        tab.text = folder.title
                        tab.tag = folder.id
                        viewBinding.folderTabs.addTab(tab)
                    }
                }
            ),
            map(
                {
                    it.listState.toListItems(
                        noDataPlaceholderProvider = {
                            PatientListItem.NoDataPlaceholder(
                                title = viewBinding.context.getString(R.string.no_patients_title),
                                description = viewBinding.context.getString(R.string.no_patients_description),
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
        viewBinding.list.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
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
        viewBinding.folderTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val id = tab.tag as? Long
                    eventSender?.sendEvent(
                        PatientListEvent.PickFolder(id)
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            }
        )
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
        override val title: String,
        override val description: String,
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
            binding.fullNameLabel.text = item.patient.fullName
            binding.infoLabel.text = item.patient.info
            binding.avatarLabel.text = item.patient.avatarText
            binding.avatarLabel.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.patient.avatarBgColor)
            }
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem.patient.id == newItem.patient.id
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)