package pixel.cando.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

fun EditText.doAfterTextChanged(
    action: (String) -> Unit
) = addTextChangedWatcher(
    afterTextChanged = {
        action.invoke(it?.toString().orEmpty())
    }
)

fun EditText.addTextChangedWatcher(
    beforeTextChanged: (
        text: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) -> Unit = { _, _, _, _ -> },
    onTextChanged: (
        text: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) -> Unit = { _, _, _, _ -> },
    afterTextChanged: (text: Editable?) -> Unit = {}
): TextWatcher {
    val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s)
        }

        override fun beforeTextChanged(
            text: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
            beforeTextChanged.invoke(
                text,
                start,
                count,
                after
            )
        }

        override fun onTextChanged(
            text: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            onTextChanged.invoke(
                text,
                start,
                before,
                count
            )
        }
    }
    addTextChangedListener(textWatcher)

    return textWatcher
}