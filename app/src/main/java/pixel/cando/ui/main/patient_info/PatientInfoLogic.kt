package pixel.cando.ui.main.patient_info

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.Gender
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.data.remote.dto.Units
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight

object PatientInfoLogic {

    fun init(
        model: PatientInfoDataModel
    ): First<PatientInfoDataModel, PatientInfoEffect> {
        if (model.patientData == null) {
            return First.first(
                model.copy(
                    loadingState = PatientInfoLoadingState.LOADING,
                ),
                setOf(
                    PatientInfoEffect.LoadData(
                        patientId = model.patientId,
                    )
                )
            )
        }
        return First.first(model)
    }

    fun update(
        model: PatientInfoDataModel,
        event: PatientInfoEvent
    ): Next<PatientInfoDataModel, PatientInfoEffect> {
        return when (event) {
            // ui
            is PatientInfoEvent.RefreshRequest -> {
                if (model.loadingState == PatientInfoLoadingState.NONE) {
                    Next.next(
                        model.copy(
                            loadingState = PatientInfoLoadingState.REFRESHING
                        ),
                        setOf(
                            PatientInfoEffect.LoadData(
                                patientId = model.patientId,
                            )
                        )
                    )
                } else Next.noChange()
            }
            is PatientInfoEvent.ExitTap -> {
                Next.dispatch(
                    setOf(
                        PatientInfoEffect.Exit
                    )
                )
            }
            // model
            is PatientInfoEvent.LoadDataSuccess -> {
                Next.next(
                    model.copy(
                        loadingState = PatientInfoLoadingState.NONE,
                        patientData = event.patientData,
                    )
                )
            }
            is PatientInfoEvent.LoadDataFailure -> {
                Next.next(
                    model.copy(
                        loadingState = PatientInfoLoadingState.NONE,
                    ),
                    setOf(
                        PatientInfoEffect.ShowUnexpectedError
                    )
                )
            }
        }
    }

    fun effectHandler(
        resourceProvider: ResourceProvider,
        messageDisplayer: MessageDisplayer,
        remoteRepository: RemoteRepository,
        flowRouter: FlowRouter,
    ): Connectable<PatientInfoEffect, PatientInfoEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is PatientInfoEffect.LoadData -> {
                    val result = remoteRepository.getPatient(effect.patientId)

                    remoteRepository.getDoctor().onLeft { doctor ->
                        result.onLeft {
                            output.accept(
                                PatientInfoEvent.LoadDataSuccess(
                                    PatientDataModel(
                                        fullName = it.fullName,
                                        gender = it.gender,
                                        age = it.age,
                                        weight = it.weight,
                                        height = it.height,
                                        phoneNumber = it.phoneNumber,
                                        email = it.email,
                                        address = it.address,
                                        country = it.country,
                                        city = it.city,
                                        postalCode = it.postalCode,
                                        units = doctor.units
                                    )
                                )
                            )
                        }
                    }
                    result.onRight {
                        logError(it)
                        output.accept(
                            PatientInfoEvent.LoadDataFailure
                        )
                    }
                }
                is PatientInfoEffect.Exit -> {
                    flowRouter.exit()
                }
                is PatientInfoEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
            }
        }

    fun initialModel(
        patientId: Long
    ) = PatientInfoDataModel(
        patientId = patientId,
        loadingState = PatientInfoLoadingState.NONE,
        patientData = null,
    )

}

sealed class PatientInfoEvent {
    // ui
    object RefreshRequest : PatientInfoEvent()
    object ExitTap : PatientInfoEvent()

    // model
    data class LoadDataSuccess(
        val patientData: PatientDataModel
    ) : PatientInfoEvent()

    object LoadDataFailure : PatientInfoEvent()
}

sealed class PatientInfoEffect {
    data class LoadData(
        val patientId: Long,
    ) : PatientInfoEffect()

    object Exit : PatientInfoEffect()

    object ShowUnexpectedError : PatientInfoEffect()
}

@Parcelize
data class PatientInfoDataModel(
    val patientId: Long,
    val loadingState: PatientInfoLoadingState,
    val patientData: PatientDataModel?,
) : Parcelable

@Parcelize
data class PatientDataModel(
    val fullName: String,
    val gender: Gender,
    val age: Int,
    val weight: Float,
    val height: Float,
    val phoneNumber: String?,
    val email: String?,
    val address: String?,
    val country: String?,
    val city: String?,
    val postalCode: String?,
    val units: Units?
) : Parcelable

enum class PatientInfoLoadingState {
    NONE, LOADING, REFRESHING
}

data class PatientInfoViewModel(
    val title: String?,
    val isRefreshing: Boolean,
    val isLoaderVisible: Boolean,
    val listItems: List<PatientInfoListItem>,
)

sealed class PatientInfoListItem : ListItem {
    data class Header(
        val title: String,
        val isFirst: Boolean,
        val isLast: Boolean,
    ) : PatientInfoListItem()

    data class InfoPortion(
        val title: String,
        val value: String,
        val isFirst: Boolean,
        val isLast: Boolean,
    ) : PatientInfoListItem()

    data class Measurement(
        val title: String,
        val value: String,
        val unit: String,
        val isFirst: Boolean,
        val isLast: Boolean,
    ) : PatientInfoListItem()
}

fun PatientInfoDataModel.viewModel(
    resourceProvider: ResourceProvider
) = PatientInfoViewModel(
    title = patientData?.fullName,
    isRefreshing = loadingState == PatientInfoLoadingState.REFRESHING,
    isLoaderVisible = loadingState == PatientInfoLoadingState.LOADING,
    listItems = patientData?.let {
        listOf(
            PatientInfoListItem.Header(
                title = resourceProvider.getString(R.string.patient_info_section_general),
                isFirst = true,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.age),
                value = it.age.toString(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.gender),
                value = resourceProvider.getString(
                    when (it.gender) {
                        Gender.MALE -> R.string.male
                        Gender.FEMALE -> R.string.female
                    }
                ),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.Measurement(
                title = resourceProvider.getString(R.string.height),
                value = it.height.toString(),
                unit = it.units?.height.orEmpty(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.Measurement(
                title = resourceProvider.getString(R.string.weight),
                value = it.weight.toString(),
                unit = it.units?.weight.orEmpty(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.Header(
                title = resourceProvider.getString(R.string.patient_info_section_contacts),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.phone),
                value = it.phoneNumber.orEmpty(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.email),
                value = it.email.orEmpty(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.address),
                value = it.address.orEmpty(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.country),
                value = it.country.orEmpty(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.city),
                value = it.city.orEmpty(),
                isFirst = false,
                isLast = false,
            ),
            PatientInfoListItem.InfoPortion(
                title = resourceProvider.getString(R.string.zip),
                value = it.postalCode.orEmpty(),
                isFirst = false,
                isLast = true,
            ),
        )
    } ?: emptyList()
)