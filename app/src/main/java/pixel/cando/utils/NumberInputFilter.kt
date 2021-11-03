package pixel.cando.utils

import android.text.InputFilter
import android.text.Spanned


class NumberInputFilter(
    maxDigitsBeforeDecimalPoint: Int,
    maxDigitsAfterDecimalPoint: Int
) : InputFilter {

    private val pattern =
        "(([1-9]{1})([0-9]{0,${maxDigitsBeforeDecimalPoint - 1}})?)?(\\.[0-9]{0,$maxDigitsAfterDecimalPoint})?"

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {

        val builder = StringBuilder(dest)
        builder.replace(
            dstart, dend, source
                .subSequence(start, end).toString()
        )

        val newString = builder.toString()

        return if (!newString.matches(pattern.toRegex())) {
            if (source.isEmpty()) dest.subSequence(dstart, dend) else ""
        } else null

    }

}

fun weightInputFilter() = NumberInputFilter(
    maxDigitsBeforeDecimalPoint = 3,
    maxDigitsAfterDecimalPoint = 1
)

fun heightInputFilter() = NumberInputFilter(
    maxDigitsBeforeDecimalPoint = 3,
    maxDigitsAfterDecimalPoint = 0
)