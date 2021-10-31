package pixel.cando.ui.main.home

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize

object HomeLogic {

    fun init(
        model: HomeDataModel
    ): First<HomeDataModel, HomeEffect> {
        return First.first(model)
    }

    fun update(
        model: HomeDataModel,
        event: HomeEvent
    ): Next<HomeDataModel, HomeEffect> {
        return when (event) {
            is HomeEvent.SelectTab -> {
                Next.next(
                    model.copy(
                        selectedIndex = event.index
                    )
                )
            }
        }
    }

    fun effectHandler(
    ): Connectable<HomeEffect, HomeEvent> {
        return Connectable<HomeEffect, HomeEvent> {
            object : Connection<HomeEffect> {
                override fun dispose() {}

                override fun accept(value: HomeEffect) {}
            }
        }
    }

    fun initialModel(
    ) = HomeDataModel(
        selectedIndex = 0
    )

}

sealed class HomeEvent {
    data class SelectTab(
        val index: Int
    ) : HomeEvent()
}

sealed class HomeEffect {

}

@Parcelize
data class HomeDataModel(
    val selectedIndex: Int
) : Parcelable

data class HomeViewModel(
    val selectedIndex: Int
)

val HomeDataModel.viewModel: HomeViewModel
    get() = HomeViewModel(
        selectedIndex = selectedIndex
    )