package pixel.cando.utils

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import pixel.cando.R

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.visibleOrGone(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

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

val ViewBinding.context: Context
    get() = this.root.context

fun RecyclerView.addLoadMoreListener(
    reversed: Boolean = false,
    threshold: Int = 5,
    onLoadMore: () -> Unit
) {
    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(
            recyclerView: RecyclerView,
            dx: Int,
            dy: Int
        ) {
            if (recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                return
            }
            val adapter = recyclerView.adapter ?: return
            if (adapter.itemCount == 0) {
                return
            }
            if (reversed) {
                val firstVisiblePosition = if (recyclerView.childCount != 0) {
                    recyclerView.getChildViewHolder(recyclerView.getChildAt(0)).adapterPosition
                } else RecyclerView.NO_POSITION
                if (firstVisiblePosition == RecyclerView.NO_POSITION) {
                    return
                }
                if (firstVisiblePosition <= threshold
                    || adapter.itemCount < threshold
                ) {
                    onLoadMore()
                }
            } else {
                val lastVisiblePosition = if (recyclerView.childCount != 0) {
                    recyclerView.getChildAdapterPosition(recyclerView.getChildAt(recyclerView.childCount - 1))
                } else RecyclerView.NO_POSITION
                if (lastVisiblePosition == RecyclerView.NO_POSITION) {
                    return
                }
                if (lastVisiblePosition >= adapter.itemCount - threshold
                    || adapter.itemCount < threshold
                ) {
                    onLoadMore()
                }
            }
        }
    })

}

fun View.dpToPx(
    dps: Float
): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dps,
        resources.displayMetrics
    )
}

fun View.setListRoundedBg(
    isFirst: Boolean,
    isLast: Boolean
) {
    background = when {
        isFirst && isLast -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_item
        )
        isFirst -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_top_item
        )
        isLast -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_bottom_item
        )
        else -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_indeterminate_item
        )
    }
}

fun View.setListRoundedBgWithDividers(
    isFirst: Boolean,
    isLast: Boolean
) {
    background = when {
        isFirst && isLast -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_item
        )
        isFirst -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_top_item_with_divider
        )
        isLast -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_bottom_item_with_divider
        )
        else -> ContextCompat.getDrawable(
            context,
            R.drawable.bg_rounded_list_indeterminate_item_with_divider
        )
    }
}

fun Fragment.hideKeyboard() {
    val inputMethodService = requireContext().getSystemService(
        Activity.INPUT_METHOD_SERVICE
    ) as InputMethodManager
    requireView().findFocus()?.apply {
        clearFocus()
        inputMethodService.hideSoftInputFromWindow(
            windowToken,
            0
        )
    }
}