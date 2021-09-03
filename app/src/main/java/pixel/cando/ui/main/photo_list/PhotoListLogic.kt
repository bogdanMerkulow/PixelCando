package pixel.cando.ui.main.photo_list

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler

object PhotoListLogic {

    fun init(
        model: PhotoListDataModel
    ): First<PhotoListDataModel, PhotoListEffect> {
        return First.first(model)
    }


    fun update(
        model: PhotoListDataModel,
        event: PhotoListEvent
    ): Next<PhotoListDataModel, PhotoListEffect> {
        return when (event) {
            is PhotoListEvent.UploadPhotoClick -> {
                Next.noChange()
            }
        }
    }

    fun effectHandler(
    ): Connectable<PhotoListEffect, PhotoListEvent> =
        CoroutineScopeEffectHandler { effect, output ->

        }

    fun initialModel(
    ) = PhotoListDataModel(
        photos = emptyList(),
        isLoading = false,
    )

}

sealed class PhotoListEvent {
    object UploadPhotoClick : PhotoListEvent()
}

sealed class PhotoListEffect {

}

@Parcelize
data class PhotoListDataModel(
    val photos: List<Unit>,
    val isLoading: Boolean,
) : Parcelable

data class PhotoListViewModel(
    val listItems: List<PhotoListItem>,
    val isLoaderVisible: Boolean,
)

sealed class PhotoListItem : ListItem {

    object NoData : PhotoListItem()

}

fun PhotoListDataModel.viewModel(
) = PhotoListViewModel(
    listItems = when {
        isLoading -> emptyList()
        photos.isEmpty() -> listOf(PhotoListItem.NoData)
        else -> emptyList() // TODO replace with mapping of photos
    },
    isLoaderVisible = isLoading,
)