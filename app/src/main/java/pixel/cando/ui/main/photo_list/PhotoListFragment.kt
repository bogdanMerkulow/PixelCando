package pixel.cando.ui.main.photo_list

import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import pixel.cando.R
import pixel.cando.data.models.PhotoState
import pixel.cando.databinding.FragmentPhotoListBinding
import pixel.cando.databinding.ListItemPhotoBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.list.createDifferAdapter
import pixel.cando.ui._base.list.createDifferAdapterDelegate
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.ui._common.noDataListPlaceholder
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoEnabled
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.intoListDifferAdapter
import pixel.cando.utils.diffuser.intoSwipeRefresh
import pixel.cando.utils.diffuser.map
import pixel.cando.utils.setListRoundedBgWithDividers
import pixel.cando.utils.visibleOrGone

class PhotoListFragment : ViewBindingFragment<FragmentPhotoListBinding>(
    FragmentPhotoListBinding::inflate
), ViewModelRender<PhotoListViewModel>,
    EventSenderNeeder<PhotoListEvent>,
    DiffuserCreator<PhotoListViewModel, FragmentPhotoListBinding>,
    DiffuserProviderNeeder<PhotoListViewModel>,
    CameraFragment.Callback {

    override var eventSender: EventSender<PhotoListEvent>? = null

    override var diffuserProvider: DiffuserProvider<PhotoListViewModel>? = null

    private val adapter by lazy {
        createDifferAdapter(
            noPhotosAdapterDelegate(),
            photoAdapterDelegate {
                eventSender?.sendEvent(
                    PhotoListEvent.DeletePhotoTap(it)
                )
            },
        )
    }

    override fun createDiffuser(
        viewBinding: FragmentPhotoListBinding
    ): Diffuser<PhotoListViewModel> {
        return Diffuser(
            map(
                { it.listItems },
                intoListDifferAdapter(adapter)
            ),
            map(
                { it.isLoaderVisible },
                intoVisibleOrGone(viewBinding.progressBar)
            ),
            map(
                { it.isTakePhotoButtonEnabled },
                intoEnabled(viewBinding.takePhotoButton)
            ),
            map(
                { it.isRefreshing },
                intoSwipeRefresh(viewBinding.swipeRefresh)
            ),
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPhotoListBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.list.setHasFixedSize(true)
        viewBinding.list.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(
                positionStart: Int,
                itemCount: Int
            ) {
                if (positionStart == 0) {
                    val firstVisibleItemPosition =
                        (viewBinding.list.layoutManager as LinearLayoutManager)
                            .findFirstVisibleItemPosition()
                    if (firstVisibleItemPosition == 0) {
                        viewBinding.list.smoothScrollToPosition(0)
                    }
                }
            }
        })
        viewBinding.takePhotoButton.setOnClickListener {
            eventSender?.sendEvent(
                PhotoListEvent.AddPhotoClick
            )
        }
        viewBinding.swipeRefresh.setOnRefreshListener {
            eventSender?.sendEvent(
                PhotoListEvent.RefreshRequest
            )
        }
    }

    override fun renderViewModel(
        viewModel: PhotoListViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onCameraResult(
        uri: Uri
    ) {
        eventSender?.sendEvent(
            PhotoListEvent.PhotoTaken(
                uri = uri
            )
        )
    }

    override fun onCameraCancel() {
    }

}

private fun noPhotosAdapterDelegate(
) = noDataListPlaceholder<
        PhotoListItem.NoData,
        PhotoListItem>()

private fun photoAdapterDelegate(
    onDeleteClick: (Long) -> Unit,
) = createDifferAdapterDelegate<
        PhotoListItem.Photo,
        PhotoListItem,
        ListItemPhotoBinding>(
    viewBindingCreator = ListItemPhotoBinding::inflate,
    viewHolderBinding = {
        binding.deleteIcon.setOnClickListener {
            onDeleteClick.invoke(item.photo.id)
        }
        bind {
            val photo = item.photo
            binding.photoImageView.load(photo.imageUrl)
            binding.noteLabel.text = photo.note
            binding.dateLabel.text = photo.date
            binding.deleteIcon.visibleOrGone(photo.mayDelete)
            when (photo.state) {
                PhotoState.PENDING -> {
                    binding.stateLabel.setText(R.string.photo_state_pending)
                    binding.stateLabel.setCompoundDrawablesWithIntrinsicBounds(
                        getDrawable(R.drawable.ic_hourglass_orange),
                        null, null, null
                    )
                    binding.stateLabel.setTextColor(
                        getColor(R.color.orange_tulip_tree)
                    )
                }
                PhotoState.ACCEPTED -> {
                    binding.stateLabel.setText(R.string.photo_state_accepted)
                    binding.stateLabel.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, null, null
                    )
                    binding.stateLabel.setTextColor(
                        getColor(R.color.blue_boston)
                    )
                }
                PhotoState.REJECTED -> {
                    binding.stateLabel.setText(R.string.photo_state_rejected)
                    binding.stateLabel.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, null, null
                    )
                    binding.stateLabel.setTextColor(
                        getColor(R.color.gray_dove)
                    )
                }
            }
            binding.root.setListRoundedBgWithDividers(
                isFirst = photo.isFirst,
                isLast = photo.isLast,
            )
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem.photo.id == newItem.photo.id
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)