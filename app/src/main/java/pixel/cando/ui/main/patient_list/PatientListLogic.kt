package pixel.cando.ui.main.patient_list

import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.ocpsoft.prettytime.PrettyTime
import pixel.cando.R
import pixel.cando.data.models.Folder
import pixel.cando.data.models.Gender
import pixel.cando.data.models.PatientListItemInfo
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui.Screens
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.list.ListAction
import pixel.cando.ui._base.list.ListState
import pixel.cando.ui._base.list.ParcelableListState
import pixel.cando.ui._base.list.isLoading
import pixel.cando.ui._base.list.listStateUpdater
import pixel.cando.ui._base.list.loadedItems
import pixel.cando.ui._base.list.map
import pixel.cando.ui._base.list.plainState
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.ui._base.tea.toFirst
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

object PatientListLogic {

    fun init(
        model: PatientListDataModel
    ): First<PatientListDataModel, PatientListEffect> {
        return when {
            model.listState is ParcelableListState.NotInitialized -> {
                val next = listUpdater.update(
                    model,
                    PatientListEvent.RefreshRequest
                )
                First.first(
                    next.modelUnsafe(),
                    next.effects()
                            + setOf(PatientListEffect.LoadFolders)
                )
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
            is PatientListEvent.FolderListLoaded -> {
                Next.next(
                    model.copy(
                        folders = event.folders,
                    )
                )
            }
            is PatientListEvent.PickFolder -> {
                if (event.folderId == model.currentFolderId)
                    return Next.noChange()
                val newModel = model.copy(
                    currentFolderId = event.folderId
                )
                listUpdater.update(
                    newModel,
                    event
                )
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
                is PatientListEvent.PickFolder -> ListAction.Restart()
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
        loadPageEffectMapper = {
            PatientListEffect.LoadPage(
                folderId = currentFolderId,
                page = it.page,
            )
        },
        emitErrorEffectMapper = {
            PatientListEffect.ShowError(
                it.error.message ?: it.error.localizedMessage
            )
        }
    )

    fun effectHandler(
        messageDisplayer: MessageDisplayer,
        remoteRepository: RemoteRepository,
        flowRouter: FlowRouter,
    ): Connectable<PatientListEffect, PatientListEvent> {
        val loadNextPageJob = AtomicReference<Job>()
        return CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PatientListEffect.LoadPage -> {
                    loadNextPageJob.getAndSet(
                        launch {
                            val result = remoteRepository.getPatients(
                                folderId = effect.folderId,
                                page = effect.page,
                            )
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
                is PatientListEffect.LoadFolders -> {
                    val result = remoteRepository.getFolders()
                    result.onLeft {
                        output.accept(
                            PatientListEvent.FolderListLoaded(
                                it.map { it.dataModel }
                            )
                        )
                    }
                }
                is PatientListEffect.NavigateToPatient -> {
                    flowRouter.navigateTo(
                        Screens.patientDetails(
                            patientId = effect.patientId,
                        )
                    )
                }
                is PatientListEffect.ShowError -> {
                    messageDisplayer.showMessage(effect.message)
                }
            }
        }
    }

    fun initialModel(
    ) = PatientListDataModel(
        currentFolderId = null,
        folders = emptyList(),
        listState = ParcelableListState.NotInitialized()
    )

}

sealed class PatientListEvent {
    // ui
    object RefreshRequest : PatientListEvent()
    object LoadNextPage : PatientListEvent()
    data class PickPatient(
        val patientId: Long,
    ) : PatientListEvent()

    data class PickFolder(
        val folderId: Long?,
    ) : PatientListEvent()

    // model
    class PatientListLoaded(
        val patients: List<PatientDataModel>,
    ) : PatientListEvent()

    class PatientListLoadFailed(
        val error: Throwable,
    ) : PatientListEvent()

    object StopListLoading : PatientListEvent()

    data class FolderListLoaded(
        val folders: List<FolderDataModel>
    ) : PatientListEvent()

}

sealed class PatientListEffect {
    data class LoadPage(
        val folderId: Long?,
        val page: Int,
    ) : PatientListEffect()

    object LoadFolders : PatientListEffect()
    data class ShowError(val message: String) : PatientListEffect()

    data class NavigateToPatient(
        val patientId: Long,
    ) : PatientListEffect()
}

@Parcelize
data class PatientDataModel(
    val id: Long,
    val fullName: String,
    val gender: Gender,
    val age: Int,
    val avatarText: String,
    val avatarBgColor: String,
    val lastExamAt: LocalDateTime?,
) : Parcelable

@Parcelize
data class FolderDataModel(
    val id: Long,
    val title: String,
) : Parcelable

@Parcelize
data class PatientListDataModel(
    val currentFolderId: Long?,
    val folders: List<FolderDataModel>,
    val listState: ParcelableListState<PatientDataModel>
) : Parcelable

data class PatientListViewModel(
    val folders: List<FolderViewModel>,
    val listState: ListState<PatientViewModel>
)

data class PatientViewModel(
    val id: Long,
    val fullName: String,
    val info: String,
    val avatarText: String,
    @ColorInt val avatarBgColor: Int,
    val date: String?,
)

data class FolderViewModel(
    val id: Long,
    val title: String,
)

fun PatientDataModel.viewModel(
    resourceProvider: ResourceProvider,
    prettyTime: PrettyTime,
) = PatientViewModel(
    id = id,
    fullName = fullName,
    info = listOf(
        resourceProvider.getString(
            when (gender) {
                Gender.MALE -> R.string.male
                Gender.FEMALE -> R.string.female
            }
        ),
        age.toString()
    ).joinToString(),
    avatarText = avatarText,
    avatarBgColor = try {
        avatarBgColor.toColorInt()
    } catch (ex: IllegalArgumentException) {
        resourceProvider.getColor(R.color.blue_boston)
    },
    date = lastExamAt?.let { prettyTime.format(it) }
)

private val FolderDataModel.viewModel: FolderViewModel
    get() = FolderViewModel(
        id = id,
        title = title,
    )

fun PatientListDataModel.viewModel(
    resourceProvider: ResourceProvider
) = PatientListViewModel(
    folders = folders.map {
        it.viewModel
    },
    listState = listState.plainState.map { patient, _, _ ->
        val prettyTime = PrettyTime()
        patient.viewModel(
            resourceProvider = resourceProvider,
            prettyTime = prettyTime,
        )
    }
)

private val PatientListItemInfo.dataModel: PatientDataModel
    get() = PatientDataModel(
        id = id,
        fullName = fullName,
        gender = gender,
        age = age,
        avatarText = avatarText,
        avatarBgColor = avatarBgColor,
        lastExamAt = lastExamAt,
    )

private val Folder.dataModel: FolderDataModel
    get() = FolderDataModel(
        id = id,
        title = title,
    )