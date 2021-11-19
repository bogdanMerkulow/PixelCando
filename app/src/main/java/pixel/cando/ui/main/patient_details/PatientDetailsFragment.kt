package pixel.cando.ui.main.patient_details

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import pixel.cando.R
import pixel.cando.databinding.FragmentPatientDetailsBinding
import pixel.cando.databinding.ListItemExamBinding
import pixel.cando.databinding.ListItemHeaderBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.list.createDifferAdapter
import pixel.cando.ui._base.list.createDifferAdapterDelegate
import pixel.cando.ui._base.list.isRefreshing
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.ui._commmon.ListInitialLoader
import pixel.cando.ui._commmon.ListMoreLoader
import pixel.cando.ui._commmon.NoDataListPlaceholder
import pixel.cando.ui._commmon.listInitialLoader
import pixel.cando.ui._commmon.listMoreLoader
import pixel.cando.ui._commmon.noDataListPlaceholder
import pixel.cando.ui._commmon.toListItems
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.ui.main.patient_photo_review.PatientPhotoReviewFragment
import pixel.cando.utils.addLoadMoreListener
import pixel.cando.utils.context
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.into
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoEnabled
import pixel.cando.utils.diffuser.ViewDiffusers.intoText
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.intoListDifferAdapter
import pixel.cando.utils.diffuser.intoSwipeRefresh
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.setListRoundedBg

class PatientDetailsFragment : ViewBindingFragment<FragmentPatientDetailsBinding>(
    FragmentPatientDetailsBinding::inflate
), ViewModelRender<PatientDetailsViewModel>,
    EventSenderNeeder<PatientDetailsEvent>,
    DiffuserCreator<PatientDetailsViewModel, FragmentPatientDetailsBinding>,
    DiffuserProviderNeeder<PatientDetailsViewModel>,
    CameraFragment.Callback,
    PatientPhotoReviewFragment.Listener {

    override var eventSender: EventSender<PatientDetailsEvent>? = null

    override var diffuserProvider: DiffuserProvider<PatientDetailsViewModel>? = null

    private val adapter by lazy {
        createDifferAdapter(
            headerAdapterDelegate(),
            examAdapterDelegate {
                eventSender?.sendEvent(
                    PatientDetailsEvent.ExamTap(it)
                )
            },
            noDataListPlaceholder<ExamListItem.NoDataPlaceholder, ExamListItem>(),
            listInitialLoader<ExamListItem.InitialLoader, ExamListItem>(),
            listMoreLoader<ExamListItem.MoreLoader, ExamListItem>(),
        )
    }

    override fun createDiffuser(
        viewBinding: FragmentPatientDetailsBinding
    ): Diffuser<PatientDetailsViewModel> {
        return Diffuser(
            map(
                { it.title },
                into {
                    viewBinding.toolbar.title = it
                }
            ),
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(
                    viewBinding.progressBar
                )
            ),
            map(
                { it.listState.isRefreshing },
                intoSwipeRefresh(viewBinding.swipeRefresh)
            ),
            map(
                { it.isTakePhotoButtonEnabled },
                intoEnabled(
                    viewBinding.takePhotoButton
                )
            ),
            map(
                {
                    it.listState.toListItems(
                        noDataPlaceholderProvider = {
                            ExamListItem.NoDataPlaceholder(
                                title = viewBinding.context.getString(R.string.no_exams_title),
                                description = viewBinding.context.getString(R.string.no_exams_description),
                            )
                        },
                        initialLoaderProvider = {
                            ExamListItem.InitialLoader
                        },
                        moreLoaderProvider = {
                            ExamListItem.MoreLoader
                        },
                        itemMapper = {
                            listOf(ExamListItem.ExamsHeader) + map {
                                ExamListItem.Exam(it)
                            }
                        }
                    )
                },
                intoListDifferAdapter(adapter)
            ),
            map(
                { it.photoToReview != null },
                intoVisibleOrGone(
                    viewBinding.reviewPhotoGroup
                )
            ),
            map(
                { it.photoToReview?.date },
                intoText(viewBinding.reviewPhotoDate)
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPatientDetailsBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)

        viewBinding.toolbar.setNavigationOnClickListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.ExitTap
            )
        }

        viewBinding.takePhotoButton.setOnClickListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.CreateExamTap
            )
        }
        viewBinding.swipeRefresh.setOnRefreshListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.RefreshRequest
            )
        }
        viewBinding.patientInfoIcon.setOnClickListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.PatientInfoTap
            )
        }
        viewBinding.reviewPhotoButton.setOnClickListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.ReviewPatientTap
            )
        }
        viewBinding.examList.setHasFixedSize(true)
        viewBinding.examList.adapter = adapter
        viewBinding.examList.addLoadMoreListener {
            eventSender?.sendEvent(
                PatientDetailsEvent.LoadExamNextPage
            )
        }

    }

    override fun renderViewModel(
        viewModel: PatientDetailsViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onCameraResult(
        uri: Uri
    ) {
        eventSender?.sendEvent(
            PatientDetailsEvent.PhotoTaken(
                uri = uri
            )
        )
    }

    override fun onCameraCancel() {
    }

    override fun onAcceptPatientPhoto() {

    }

    override fun onRejectPatientPhoto(
        reason: String
    ) {
    }
}

private sealed class ExamListItem : ListItem {
    object InitialLoader : ExamListItem(),
        ListInitialLoader

    object MoreLoader : ExamListItem(),
        ListMoreLoader

    data class NoDataPlaceholder(
        override val title: String,
        override val description: String,
    ) : ExamListItem(),
        NoDataListPlaceholder

    object ExamsHeader : ExamListItem()

    data class Exam(
        val exam: ExamViewModel
    ) : ExamListItem()
}

private fun examAdapterDelegate(
    clickListener: (Long) -> Unit
) = createDifferAdapterDelegate<
        ExamListItem.Exam,
        ExamListItem,
        ListItemExamBinding>(
    viewBindingCreator = ListItemExamBinding::inflate,
    viewHolderBinding = {
        binding.root.setOnClickListener {
            clickListener.invoke(item.exam.id)
        }
        bind {
            binding.weightLabel.text = item.exam.weight
            binding.fatMassLabel.text = item.exam.fatMass
            binding.fatFreeMassLabel.text = item.exam.fatFreeMass
            binding.abdominalFatMassLabel.text = item.exam.abdominalFatMass
            binding.bmiLabel.text = item.exam.bmi
            binding.dateLabel.text = item.exam.date
            if (item.exam.isStarMarked) {
                binding.numberLabel.text = null
                binding.numberLabel.setBackgroundResource(
                    R.drawable.ic_star_orange_circle
                )
            } else {
                binding.numberLabel.text = item.exam.number
                binding.numberLabel.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(
                        getColor(R.color.blue_malibu)
                    )
                }
            }
            binding.root.setListRoundedBg(
                isFirst = false,
                isLast = item.exam.isLast,
            )
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem.exam.id == newItem.exam.id
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun headerAdapterDelegate(
) = createDifferAdapterDelegate<
        ExamListItem.ExamsHeader,
        ExamListItem,
        ListItemHeaderBinding>(
    viewBindingCreator = ListItemHeaderBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.titleLabel.text = getString(R.string.exams_header_title)
            binding.root.setListRoundedBg(
                isFirst = true,
                isLast = false,
            )
        }
    },
    areItemsTheSame = { _, _ -> true },
    areContentsTheSame = { _, _ -> true },
)