package pixel.cando.ui.main.exam_details

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import pixel.cando.R
import pixel.cando.data.models.ExamUnits
import pixel.cando.databinding.FragmentExamNumericMeasurementValuesBinding
import pixel.cando.databinding.ListItemHeaderBinding
import pixel.cando.databinding.ListItemNumericMeasurementValueBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.ui._base.list.ListItem
import pixel.cando.ui._base.list.createDifferAdapter
import pixel.cando.ui._base.list.createDifferAdapterDelegate
import pixel.cando.utils.setListRoundedBg
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExamNumericMeasurementValuesFragment :
    ViewBindingFragment<FragmentExamNumericMeasurementValuesBinding>(
        FragmentExamNumericMeasurementValuesBinding::inflate
    ) {

    @Parcelize
    data class Arguments(
        val createdAt: LocalDateTime,
        val weight: Float,
        val bmi: Float,
        val bmr: Float,
        val fm: Float,
        val ffm: Float,
        val abdominalFatMass: Float,
        val tbw: Float,
        val hip: Float,
        val belly: Float,
        val waistToHeight: Float,
        val units: ExamUnits
    ) : Parcelable

    companion object {
        private const val ARG_MEASUREMENT_VALUES = "ARG_MEASUREMENT_VALUES"
        fun newInstance(
            arguments: Arguments
        ): ExamNumericMeasurementValuesFragment {
            val args = Bundle()
            args.putParcelable(ARG_MEASUREMENT_VALUES, arguments)
            val fragment = ExamNumericMeasurementValuesFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val adapter = createDifferAdapter(
        headerAdapterDelegate(),
        measurementAdapterDelegate(),
    )

    override fun onViewBindingCreated(
        viewBinding: FragmentExamNumericMeasurementValuesBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.list.setHasFixedSize(true)
        viewBinding.list.adapter = adapter
        adapter.items = requireArguments()
            .getParcelable<Arguments>(ARG_MEASUREMENT_VALUES)!!
            .toListItems(
                requireContext()
            )
    }

}

sealed class ExamDetailsListItem : ListItem {
    data class Header(
        val title: String,
        val isFirst: Boolean,
        val isLast: Boolean,
    ) : ExamDetailsListItem()

    data class Measurement(
        val title: String,
        val value: String,
        val unit: String,
        val isFirst: Boolean,
        val isLast: Boolean,
    ) : ExamDetailsListItem()
}

private fun headerAdapterDelegate(
) = createDifferAdapterDelegate<
        ExamDetailsListItem.Header,
        ExamDetailsListItem,
        ListItemHeaderBinding>(
    viewBindingCreator = ListItemHeaderBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.titleLabel.text = item.title
            binding.root.setListRoundedBg(
                isFirst = item.isFirst,
                isLast = item.isLast,
            )
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem == newItem
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun measurementAdapterDelegate(
) = createDifferAdapterDelegate<
        ExamDetailsListItem.Measurement,
        ExamDetailsListItem,
        ListItemNumericMeasurementValueBinding>(
    viewBindingCreator = ListItemNumericMeasurementValueBinding::inflate,
    viewHolderBinding = {
        bind {
            binding.titleLabel.text = item.title
            binding.valueLabel.text = item.value
            binding.unitLabel.text = item.unit
            binding.root.setListRoundedBg(
                isFirst = item.isFirst,
                isLast = item.isLast,
            )
        }
    },
    areItemsTheSame = { oldItem, newItem ->
        oldItem == newItem
    },
    areContentsTheSame = { oldItem, newItem ->
        oldItem == newItem
    }
)

private fun ExamNumericMeasurementValuesFragment.Arguments.toListItems(
    context: Context
): List<ExamDetailsListItem> {
    return listOf(
        ExamDetailsListItem.Header(
            context.getString(R.string.exam_details_measurement_values_section_params),
            isFirst = true,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.created_at),
            value = DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm")
                .format(createdAt),
            unit = "",
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.weight),
            value = weight.toString(),
            unit = units.weight,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.bmi),
            value = bmi.toString(),
            unit = units.bmi,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.bmr),
            value = bmr.toString(),
            unit = units.bmr,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.fat_mass),
            value = fm.toString(),
            unit = units.fm,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.fat_free_mass),
            value = ffm.toString(),
            unit = units.ffm,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.abdominal_fat_mass),
            value = abdominalFatMass.toString(),
            unit = units.abdominalFm,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.tbw),
            value = tbw.toString(),
            unit = units.tbw,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Header(
            context.getString(R.string.exam_details_measurement_values_section_measures),
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.hip),
            value = hip.toString(),
            unit = units.hip,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.belly),
            value = belly.toString(),
            unit = units.belly,
            isFirst = false,
            isLast = false,
        ),
        ExamDetailsListItem.Measurement(
            title = context.getString(R.string.waist_to_height),
            value = waistToHeight.toString(),
            unit = units.waistToHeight ?: context.getString(R.string.percent),
            isFirst = false,
            isLast = true,
        ),
    )
}