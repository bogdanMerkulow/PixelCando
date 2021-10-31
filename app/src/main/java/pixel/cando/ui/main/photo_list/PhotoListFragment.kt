package pixel.cando.ui.main.photo_list

import android.net.Uri
import android.os.Bundle
import pixel.cando.databinding.FragmentPhotoListBinding
import pixel.cando.databinding.ListItemNoPhotosBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.list.createDifferAdapter
import pixel.cando.ui._base.list.createDifferAdapterDelegate
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.ui.main.camera.CameraFragment
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.diffuser.ViewDiffusers.intoVisibleOrGone
import pixel.cando.utils.diffuser.intoListDifferAdapter
import pixel.cando.utils.diffuser.map

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
            noPhotosAdapterDelegate {
                eventSender?.sendEvent(
                    PhotoListEvent.UploadPhotoClick
                )
            }
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
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPhotoListBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.list.setHasFixedSize(true)
        viewBinding.list.adapter = adapter
    }

    override fun renderViewModel(
        viewModel: PhotoListViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onCameraResult(
        uri: Uri
    ) {
//        eventSender?.sendEvent(
//            PhotoListEvent.PhotoTaken(
//                bitmap = bitmap
//            )
//        )
    }

    override fun onCameraCancel() {
    }

}

private fun noPhotosAdapterDelegate(
    onUploadClick: () -> Unit,
) = createDifferAdapterDelegate<
        PhotoListItem.NoData,
        PhotoListItem,
        ListItemNoPhotosBinding>(
    viewBindingCreator = ListItemNoPhotosBinding::inflate,
    viewHolderBinding = {
        binding.uploadButton.setOnClickListener {
            onUploadClick.invoke()
        }
    },
    areItemsTheSame = { _, _ -> true },
    areContentsTheSame = { _, _ -> true },
)