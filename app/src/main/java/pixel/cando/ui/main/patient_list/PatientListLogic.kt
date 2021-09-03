package pixel.cando.ui.main.patient_list

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import pixel.cando.data.models.PatientBriefInfo
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.list.*
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui._base.tea.toFirst
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight
import java.util.concurrent.atomic.AtomicReference

object PatientListLogic {

    fun init(
        model: PatientListDataModel
    ): First<PatientListDataModel, PatientListEffect> {
        return when {
            model.listState is ParcelableListState.NotInitialized -> {
                listUpdater.update(
                    model,
                    PatientListEvent.RefreshRequest
                ).toFirst
            }
            model.listState.isLoading -> {
                listUpdater.update(
                    model,
                    PatientListEvent.StopListLoading
                ).toFirst
            }
            else -> {
                First.first(
                    model
                )
            }
        }
    }

    fun update(
        model: PatientListDataModel,
        event: PatientListEvent
    ): Next<PatientListDataModel, PatientListEffect> {
        return when (event) {
            is PatientListEvent.RefreshRequest,
            is PatientListEvent.PatientListLoaded,
            is PatientListEvent.PatientListLoadFailed,
            is PatientListEvent.LoadNextPage,
            is PatientListEvent.StopListLoading -> {
                listUpdater.update(
                    model,
                    event
                )
            }
            is PatientListEvent.PickPatient -> {
                val patient = model.listState.loadedItems().firstOrNull {
                    it.id == event.patientId
                }
                if (patient != null) {
                    Next.dispatch(
                        setOf(
                            PatientListEffect.NavigateToPatient(
                                patientId = patient.id,
                            )
                        )
                    )
                } else {
                    Next.noChange()
                }
            }
        }
    }

    private val listUpdater = listStateUpdater<
            PatientListDataModel,
            PatientListEvent,
            PatientListEffect,
            PatientDataModel>(
        listStateExtractor = { listState },
        eventMapper = {
            when (it) {
                is PatientListEvent.RefreshRequest -> ListAction.Refresh()
                is PatientListEvent.PatientListLoaded -> {
                    if (it.patients.isNotEmpty()) ListAction.PageLoaded(it.patients)
                    else ListAction.EmptyPageLoaded()
                }
                is PatientListEvent.PatientListLoadFailed -> ListAction.PageLoadFailed(it.error)
                is PatientListEvent.LoadNextPage -> ListAction.LoadMore()
                is PatientListEvent.StopListLoading -> ListAction.StopLoading()
                else -> null
            }
        },
        modelUpdater = { copy(listState = it) },
        loadPageEffectMapper = { PatientListEffect.LoadPage(it.page) },
        emitErrorEffectMapper = {
            PatientListEffect.ShowError(
                it.error.message ?: it.error.localizedMessage
            )
        }
    )

    fun effectHandler(
        messageDisplayer: MessageDisplayer,
        remoteRepository: RemoteRepository,
    ): Connectable<PatientListEffect, PatientListEvent> {
        val loadNextPageJob = AtomicReference<Job>()
        return CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PatientListEffect.LoadPage -> {
                    loadNextPageJob.getAndSet(
                        launch {
                            val result = remoteRepository.getPatients(effect.page)
                            result.onLeft {
                                output.accept(
                                    PatientListEvent.PatientListLoaded(
                                        it.map { it.dataModel }
                                    )
                                )
                            }
                            result.onRight {
                                output.accept(
                                    PatientListEvent.PatientListLoadFailed(it)
                                )
                            }
                        }
                    )?.cancel()
                }
                is PatientListEffect.NavigateToPatient -> {
                    //TODO
                }
                is PatientListEffect.ShowError -> {
                    messageDisplayer.showMessage(effect.message)
                }
            }
        }
    }

    fun initialModel(
    ) = PatientListDataModel(
        listState = ParcelableListState.NotInitialized()
    )

}

sealed class PatientListEvent {
    // ui
    object RefreshRequest : PatientListEvent()
    object LoadNextPage : PatientListEvent()
    data class PickPatient(
        val patientId: Long
    ) : PatientListEvent()

    // model
    class PatientListLoaded(val patients: List<PatientDataModel>) : PatientListEvent()
    class PatientListLoadFailed(val error: Throwable) : PatientListEvent()
    object StopListLoading : PatientListEvent()
}

sealed class PatientListEffect {
    data class LoadPage(val page: Int) : PatientListEffect()
    data class ShowError(val message: String) : PatientListEffect()

    data class NavigateToPatient(
        val patientId: Long,
    ) : PatientListEffect()
}

@Parcelize
data class PatientDataModel(
    val id: Long,
    val fullName: String,
) : Parcelable

@Parcelize
data class PatientListDataModel(
    val listState: ParcelableListState<PatientDataModel>
) : Parcelable

data class PatientListViewModel(
    val listState: ListState<PatientViewModel>
)

data class PatientViewModel(
    val id: Long,
    val fullName: String,
)

val PatientDataModel.viewModel: PatientViewModel
    get() = PatientViewModel(
        id = id,
        fullName = fullName
    )

val PatientListDataModel.viewModel: PatientListViewModel
    get() = PatientListViewModel(
        listState = listState.plainState.map { patient, _, _ ->
            patient.viewModel
        }
    )

private val PatientBriefInfo.dataModel: PatientDataModel
    get() = PatientDataModel(
        id = id,
        fullName = fullName
    )