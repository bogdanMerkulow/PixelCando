package pixel.cando.utils.diffuser;


import android.view.View;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static pixel.cando.utils.diffuser.Diffuser.into;
import static pixel.cando.utils.diffuser.Diffuser.intoAlways;
import static pixel.cando.utils.diffuser.Diffuser.map;

public class ViewDiffusers {

    /**
     * Create a Diffuser which enables user-interaction on a {@link View} when its input is true, and
     * disables user-interaction when its input is false.
     *
     * @param view - The view managed by this Diffuser.
     * @return A Diffuser which toggles the enabled state of a view based on its input.
     */
    public static Diffuser<Boolean> intoEnabled(View view) {
        return into(view::setEnabled);
    }

    /**
     * Create a Diffuser which disables user-interaction on a {@link View} when its input is true, and
     * enables user-interaction when its input is false.
     *
     * @param view - The view managed by this Diffuser.
     * @return A Diffuser which toggles the disabled state of a view based on its input.
     */
    public static Diffuser<Boolean> intoDisabled(View view) {
        return map(b -> !b, intoEnabled(view));
    }

    /**
     * Create a Diffuser which toggles a View's visibility based on its input. It will use the {@code
     * enabledVisibility} when true, and the {@code disabledVisibility} when false.
     *
     * @param enabledVisibility  - The visibility to use when the Diffuser's input is true.
     * @param disabledVisibility - The visibility to use when the Diffuser's input is false.
     * @param view               - The View which is managed by this Diffuser
     * @return A Diffuser which toggles a View's visibility based on its input.
     */
    public static Diffuser<Boolean> intoVisibility(
            @Visibility int enabledVisibility, @Visibility int disabledVisibility, View view) {
        return map(b -> b ? enabledVisibility : disabledVisibility, into(view::setVisibility));
    }

    /**
     * Create a Diffuser which will make a View visible when its input is true, and invisible when
     * false.
     *
     * @param view - The {@link View} managed by this Diffuser.
     * @return A Diffuser which toggles its View's visibility.
     */
    public static Diffuser<Boolean> intoVisibleOrInvisible(View view) {
        return intoVisibility(VISIBLE, INVISIBLE, view);
    }

    /**
     * Create a Diffuser which will make a View visible when its input is true, and set its visibility
     * to gone when false.
     *
     * @param view - The {@link View} managed by this Diffuser.
     * @return A Diffuser which toggles its View's visibility.
     */
    public static Diffuser<Boolean> intoVisibleOrGone(View view) {
        return intoVisibility(VISIBLE, GONE, view);
    }

    /**
     * Create a Diffuser which updates the text of its {@link TextView} based on its input.
     *
     * @param textView - the {@link TextView} managed by this Diffuser.
     * @return A Diffuser which sets the text of a {@link TextView}.
     */
    public static Diffuser<? super CharSequence> intoText(TextView textView) {
        return into(textView::setText);
    }

    /**
     * Create a Diffuser which updates the text of its {@link TextView} based on its input. The text
     * is read from the supplied text resource identifier.
     *
     * @param textView - the {@link TextView} managed by this Diffuser.
     * @return A Diffuser which sets the text of a {@link TextView}.
     */
    public static Diffuser<Integer> intoTextRes(TextView textView) {
        return into(textView::setText);
    }

    /**
     * Create a Diffuser<T> which will feed a List<T> into a ListAdapter.
     *
     * @param listAdapter - The list adapter that should be updated when the Diffuser receives input.
     * @param <T>         The type of items in the list
     * @param <VH>        The type of the ListAdapter's ViewHolder
     * @return A Diffuser which supplies a ListAdapter with data.
     */
    public static <T, VH extends RecyclerView.ViewHolder> Diffuser<List<T>> intoListAdapter(
            ListAdapter<T, VH> listAdapter
    ) {
        return intoAlways(listAdapter::submitList);
    }

    @IntDef({VISIBLE, INVISIBLE, GONE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Visibility {
    }

}

