package pixel.cando.ui.main.exam_details

import android.os.Parcelable
import com.spotify.mobius.Connectable
import com.spotify.mobius.First
import com.spotify.mobius.Next
import kotlinx.coroutines.async
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.ExamUnits
import pixel.cando.data.models.Units
import pixel.cando.data.remote.RemoteRepository
import pixel.cando.ui._base.fragment.FlowRouter
import pixel.cando.ui._base.tea.CoroutineScopeEffectHandler
import pixel.cando.utils.MessageDisplayer
import pixel.cando.utils.ResourceProvider
import pixel.cando.utils.logError
import pixel.cando.utils.onLeft
import pixel.cando.utils.onRight
import java.time.LocalDateTime

object ExamDetailsLogic {

    fun init(
        model: ExamDetailsDataModel
    ): First<ExamDetailsDataModel, ExamDetailsEffect> {
        if (model.loadedData == null) {
            return First.first(
                model.copy(
                    isLoading = true
                ),
                setOf(
                    ExamDetailsEffect.LoadData(
                        examId = model.examId,
                        patientId = model.patientId,
                    )
                )
            )
        }
        return First.first(model)
    }

    fun update(
        model: ExamDetailsDataModel,
        event: ExamDetailsEvent
    ): Next<ExamDetailsDataModel, ExamDetailsEffect> {
        return when (event) {
            // ui
            ExamDetailsEvent.ExitTap -> {
                Next.dispatch(
                    setOf(
                        ExamDetailsEffect.Exit
                    )
                )
            }
            // model
            is ExamDetailsEvent.LoadDataSuccess -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                        loadedData = event.data,
                    )
                )
            }
            is ExamDetailsEvent.LoadDataFailure -> {
                Next.next(
                    model.copy(
                        isLoading = false,
                    ),
                    setOf(
                        ExamDetailsEffect.ShowUnexpectedError
                    )
                )
            }
        }
    }

    fun effectHandler(
        messageDisplayer: MessageDisplayer,
        resourceProvider: ResourceProvider,
        remoteRepository: RemoteRepository,
        flowRouter: FlowRouter,
    ): Connectable<ExamDetailsEffect, ExamDetailsEvent> =
        CoroutineScopeEffectHandler { effect, output ->
            when (effect) {
                is ExamDetailsEffect.LoadData -> {

                    val examDeferred = async { remoteRepository.getExam(effect.examId) }
                    val patientDeferred = async { remoteRepository.getPatient(effect.patientId) }
                    val doctorDeferred = async { remoteRepository.getDoctor() }

                    val examResult = examDeferred.await()
                    val patientResult = patientDeferred.await()
                    val doctorResult = doctorDeferred.await()

                    doctorResult.onLeft { doctor ->
                        examResult.onLeft { exam ->
                            patientResult.onLeft { patient ->
                                output.accept(
                                    ExamDetailsEvent.LoadDataSuccess(
                                        ExamDetailsLoadedDataModel(
                                            patientFullName = patient.fullName,
                                            examCreatedAt = exam.createdAt,
                                            examNumber = exam.number,
                                            weight = exam.weight,
                                            bmi = exam.bmi,
                                            bmr = exam.bmr,
                                            fm = exam.fm,
                                            ffm = exam.ffm,
                                            abdominalFatMass = exam.abdominalFatMass,
                                            tbw = exam.tbw,
                                            hip = exam.hip,
                                            belly = exam.belly,
                                            waistToHeight = exam.waistToHeight,
                                            silhouetteUrl = exam.silhouetteUrl,
                                            examUnits = doctor.units.toExamUnits()
                                        )
                                    )
                                )
                            }
                            patientResult.onRight {
                                logError(it)
                                output.accept(
                                    ExamDetailsEvent.LoadDataFailure
                                )
                            }
                        }
                    }
                    examResult.onRight {
                        logError(it)
                        output.accept(
                            ExamDetailsEvent.LoadDataFailure
                        )
                    }
                    doctorResult.onRight {
                        logError(it)
                        output.accept(
                            ExamDetailsEvent.LoadDataFailure
                        )
                    }
                }
                is ExamDetailsEffect.Exit -> {
                    flowRouter.exit()
                }
                is ExamDetailsEffect.ShowUnexpectedError -> {
                    messageDisplayer.showMessage(
                        resourceProvider.getString(R.string.something_went_wrong)
                    )
                }
            }
        }

    fun initialModel(
        examId: Long,
        patientId: Long,
    ) = ExamDetailsDataModel(
        examId = examId,
        patientId = patientId,
        isLoading = false,
        loadedData = null,
    )

}

sealed class ExamDetailsEvent {
    // ui
    object ExitTap : ExamDetailsEvent()

    // model
    data class LoadDataSuccess(
        val data: ExamDetailsLoadedDataModel
    ) : ExamDetailsEvent()

    object LoadDataFailure : ExamDetailsEvent()
}

sealed class ExamDetailsEffect {
    data class LoadData(
        val examId: Long,
        val patientId: Long,
    ) : ExamDetailsEffect()

    object Exit : ExamDetailsEffect()

    object ShowUnexpectedError : ExamDetailsEffect()
}

@Parcelize
data class ExamDetailsDataModel(
    val examId: Long,
    val patientId: Long,
    val isLoading: Boolean,
    val loadedData: ExamDetailsLoadedDataModel?,
) : Parcelable

@Parcelize
data class ExamDetailsLoadedDataModel(
    val patientFullName: String,
    val examCreatedAt: LocalDateTime,
    val examNumber: Int,
    val weight: String,
    val bmi: String,
    val bmr: String,
    val fm: String,
    val ffm: String,
    val abdominalFatMass: String,
    val tbw: String,
    val hip: String,
    val belly: String,
    val waistToHeight: String,
    val silhouetteUrl: String?,
    val examUnits: ExamUnits
) : Parcelable

fun Units.toExamUnits() = ExamUnits(
    bmr = bmr,
    bmi = bmi,
    waistToHeight = waistToHeight,
    fm = fm,
    ffm = ffm,
    hip = hip,
    tbw = tbw,
    belly = belly,
    height = height,
    weight = weight,
    abdominalFm = abdominalFm
)

fun ExamUnits.toUnits() = Units(
    bmr = bmr,
    bmi = bmi,
    waistToHeight = waistToHeight,
    fm = fm,
    ffm = ffm,
    hip = hip,
    tbw = tbw,
    belly = belly,
    height = height,
    weight = weight,
    abdominalFm = abdominalFm
)

data class ExamDetailsViewModel(
    val title: String?,
    val tabs: List<ExamDetailsTabViewModel>,
    val areTabsVisible: Boolean,
    val isLoaderVisible: Boolean,
)

sealed class ExamDetailsTabViewModel {
    abstract val title: String

    data class NumericMeasurementValues(
        override val title: String,
        val createdAt: LocalDateTime,
        val weight: String,
        val bmi: String,
        val bmr: String,
        val fm: String,
        val ffm: String,
        val abdominalFatMass: String,
        val tbw: String,
        val hip: String,
        val belly: String,
        val waistToHeight: String,
        val units: Units
    ) : ExamDetailsTabViewModel()

    data class Silhouette(
        override val title: String,
        val silhouetteUrl: String,
    ) : ExamDetailsTabViewModel()
}

fun ExamDetailsDataModel.viewModel(
    resourceProvider: ResourceProvider
): ExamDetailsViewModel {
    val tabs = loadedData?.let {
        val list = mutableListOf<ExamDetailsTabViewModel>()
        list.add(
            ExamDetailsTabViewModel.NumericMeasurementValues(
                title = resourceProvider.getString(R.string.exam_details_tab_details),
                createdAt = it.examCreatedAt,
                weight = it.weight,
                bmi = it.bmi,
                bmr = it.bmr,
                fm = it.fm,
                ffm = it.ffm,
                abdominalFatMass = it.abdominalFatMass,
                tbw = it.tbw,
                hip = it.hip,
                belly = it.belly,
                waistToHeight = it.waistToHeight,
                units = it.examUnits.toUnits()
            )
        )
        if (it.silhouetteUrl != null) {
            list.add(
                ExamDetailsTabViewModel.Silhouette(
                    title = resourceProvider.getString(R.string.exam_details_tab_silhouette),
                    silhouetteUrl = it.silhouetteUrl
                )
            )
        }
        list
    } ?: emptyList()
    val title = loadedData?.let {
        resourceProvider.getString(
            R.string.exam_details_screen_title,
            it.patientFullName,
            it.examNumber,
        )
    }
    return ExamDetailsViewModel(
        title = title,
        tabs = tabs,
        areTabsVisible = isLoading.not() && tabs.size > 1,
        isLoaderVisible = isLoading,
    )
}