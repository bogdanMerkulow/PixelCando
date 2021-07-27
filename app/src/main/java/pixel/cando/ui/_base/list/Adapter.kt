package pixel.cando.ui._base.list

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate
import com.hannesdorfmann.adapterdelegates4.AsyncListDifferDelegationAdapter
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import pixel.cando.ui._base.fragment.ViewBindingCreator

internal inline fun <CI : ListItem, reified C : CI, reified P : ListItem, V : ViewBinding> createDifferAdapterDelegateInterfaceRestricted(
    noinline viewBindingCreator: ViewBindingCreator<V>,
    noinline viewHolderBinding: ViewHolderBinding<C, V>.() -> Unit,
    noinline areItemsTheSame: (oldItem: C, newItem: C) -> Boolean,
    noinline areContentsTheSame: (oldItem: C, newItem: C) -> Boolean,
) = DifferAdapterDelegate(
    adapterDelegate = adapterDelegateViewBinding<P, P, V>(
        viewBinding = { layoutInflater, viewGroup ->
            viewBindingCreator.invoke(
                layoutInflater,
                viewGroup,
                false
            )
        },
        on = { item, _, _ ->
            item is C
        },
        block = {
            viewHolderBinding.invoke(
                DowncastingViewHolderBinding(this)
            )
        }
    ),
    itemComparator = createDowncastingItemComparator(
        areItemsTheSame = areItemsTheSame,
        areContentsTheSame = areContentsTheSame
    )
)

internal inline fun <reified C : P, P : ListItem, V : ViewBinding> createDifferAdapterDelegate(
    noinline viewBindingCreator: ViewBindingCreator<V>,
    noinline viewHolderBinding: ViewHolderBinding<C, V>.() -> Unit,
    noinline areItemsTheSame: (oldItem: C, newItem: C) -> Boolean,
    noinline areContentsTheSame: (oldItem: C, newItem: C) -> Boolean,
) = DifferAdapterDelegate(
    adapterDelegate = adapterDelegateViewBinding<C, P, V>(
        viewBinding = { layoutInflater, viewGroup ->
            viewBindingCreator.invoke(
                layoutInflater,
                viewGroup,
                false
            )
        },
        block = {
            viewHolderBinding.invoke(
                SimpleViewHolderBinding(this)
            )
        }
    ),
    itemComparator = createInheritanceRespectingItemComparator(
        areItemsTheSame = areItemsTheSame,
        areContentsTheSame = areContentsTheSame
    )
)

fun <P : ListItem> createDifferAdapter(
    vararg items: DifferAdapterDelegate<P>
): AsyncListDifferDelegationAdapter<P> {
    val delegates = items.map { it.adapterDelegate }.toTypedArray()
    val diffCallback = diffCallback(*items.map { it.itemComparator }.toTypedArray())
    return AsyncListDifferDelegationAdapter(
        diffCallback,
        *delegates
    )
}

class DifferAdapterDelegate<P : ListItem>(
    val adapterDelegate: AdapterDelegate<List<P>>,
    val itemComparator: ItemComparator<P>
)

private fun <P : Any> diffCallback(
    vararg itemComparators: ItemComparator<P>
): DiffUtil.ItemCallback<P> {

    return object : DiffUtil.ItemCallback<P>() {

        override fun areItemsTheSame(
            oldItem: P,
            newItem: P
        ): Boolean {
            return if (newItem::class == oldItem::class) {
                val comparator = itemComparators.firstOrNull {
                    it.isApplicableTo(
                        oldItem = oldItem,
                        newItem = newItem
                    )
                }
                    ?: error("Can not find a comparator for ${newItem.javaClass.name} and ${oldItem.javaClass.name}")
                comparator.areItemsTheSame(
                    oldItem,
                    newItem
                )
            } else {
                false
            }
        }

        override fun areContentsTheSame(
            oldItem: P,
            newItem: P
        ): Boolean {
            return if (newItem::class == oldItem::class) {
                val comparator = itemComparators.firstOrNull {
                    it.isApplicableTo(
                        oldItem = oldItem,
                        newItem = newItem
                    )
                }
                    ?: error("Can not find a comparator for ${newItem.javaClass.name} and ${oldItem.javaClass.name}")
                comparator.areContentsTheSame(
                    oldItem,
                    newItem
                )
            } else {
                false
            }
        }
    }
}

private inline fun <reified C : P, P : Any> createInheritanceRespectingItemComparator(
    noinline areItemsTheSame: (oldItem: C, newItem: C) -> Boolean,
    noinline areContentsTheSame: (oldItem: C, newItem: C) -> Boolean
) = object : ItemComparator<P> {

    override fun isApplicableTo(
        oldItem: P,
        newItem: P
    ): Boolean = oldItem as? C != null
            && newItem as? C != null

    override fun areItemsTheSame(
        oldItem: P,
        newItem: P
    ): Boolean = areItemsTheSame.invoke(
        oldItem as C,
        newItem as C
    )

    override fun areContentsTheSame(
        oldItem: P,
        newItem: P
    ): Boolean = areContentsTheSame.invoke(
        oldItem as C,
        newItem as C
    )

}

private inline fun <reified C, P> createDowncastingItemComparator(
    noinline areItemsTheSame: (oldItem: C, newItem: C) -> Boolean,
    noinline areContentsTheSame: (oldItem: C, newItem: C) -> Boolean
) = object : ItemComparator<P> {

    override fun isApplicableTo(
        oldItem: P,
        newItem: P
    ): Boolean = oldItem as? C != null
            && newItem as? C != null

    override fun areItemsTheSame(
        oldItem: P,
        newItem: P
    ): Boolean = areItemsTheSame.invoke(
        oldItem as C,
        newItem as C
    )

    override fun areContentsTheSame(
        oldItem: P,
        newItem: P
    ): Boolean = areContentsTheSame.invoke(
        oldItem as C,
        newItem as C
    )

}

interface ItemComparator<P> {

    fun isApplicableTo(
        oldItem: P,
        newItem: P
    ): Boolean

    fun areItemsTheSame(
        oldItem: P,
        newItem: P
    ): Boolean

    fun areContentsTheSame(
        oldItem: P,
        newItem: P
    ): Boolean

}

interface ListItem

interface ViewHolderBinding<T, V : ViewBinding> {

    val binding: V

    val context: Context

    val item: T

    fun bind(
        bindingBlock: (List<Any>) -> Unit
    )

    @ColorInt
    fun getColor(
        @ColorRes id: Int
    ): Int

    fun getColorStateList(
        @ColorRes id: Int
    ): ColorStateList?

    fun getDrawable(
        @DrawableRes id: Int
    ): Drawable?

    fun getString(
        @StringRes resId: Int
    ): String

    fun getString(
        @StringRes resId: Int,
        vararg formatArgs: Any
    ): String

    fun onFailedToRecycleView(
        block: () -> Boolean
    )

    fun onViewAttachedToWindow(
        block: () -> Unit
    )

    fun onViewDetachedFromWindow(
        block: () -> Unit
    )

    fun onViewRecycled(
        block: () -> Unit
    )

}

class SimpleViewHolderBinding<T, V : ViewBinding>(
    private val adapterDelegateViewBindingViewHolder: AdapterDelegateViewBindingViewHolder<T, V>
) : ViewHolderBinding<T, V> {

    override val binding: V get() = adapterDelegateViewBindingViewHolder.binding

    override val context: Context get() = adapterDelegateViewBindingViewHolder.context

    override val item: T get() = adapterDelegateViewBindingViewHolder.item

    override fun bind(
        bindingBlock: (List<Any>) -> Unit
    ) = adapterDelegateViewBindingViewHolder.bind(bindingBlock)

    @ColorInt
    override fun getColor(
        @ColorRes id: Int
    ): Int = adapterDelegateViewBindingViewHolder.getColor(id)

    override fun getColorStateList(
        @ColorRes id: Int
    ): ColorStateList? = adapterDelegateViewBindingViewHolder.getColorStateList(id)

    override fun getDrawable(
        @DrawableRes id: Int
    ): Drawable? = adapterDelegateViewBindingViewHolder.getDrawable(id)

    override fun getString(
        @StringRes resId: Int
    ): String = adapterDelegateViewBindingViewHolder.getString(resId)

    override fun getString(
        @StringRes resId: Int,
        vararg formatArgs: Any
    ): String = adapterDelegateViewBindingViewHolder.getString(
        resId,
        formatArgs
    )

    override fun onFailedToRecycleView(
        block: () -> Boolean
    ) = adapterDelegateViewBindingViewHolder.onFailedToRecycleView(block)

    override fun onViewAttachedToWindow(
        block: () -> Unit
    ) = adapterDelegateViewBindingViewHolder.onViewAttachedToWindow(block)

    override fun onViewDetachedFromWindow(
        block: () -> Unit
    ) = adapterDelegateViewBindingViewHolder.onViewDetachedFromWindow(block)

    override fun onViewRecycled(
        block: () -> Unit
    ) = adapterDelegateViewBindingViewHolder.onViewRecycled(block)

}

class DowncastingViewHolderBinding<C, P, V : ViewBinding>(
    private val adapterDelegateViewBindingViewHolder: AdapterDelegateViewBindingViewHolder<P, V>
) : ViewHolderBinding<C, V> {

    override val binding: V get() = adapterDelegateViewBindingViewHolder.binding

    override val context: Context get() = adapterDelegateViewBindingViewHolder.context

    override val item: C get() = adapterDelegateViewBindingViewHolder.item as C

    override fun bind(
        bindingBlock: (List<Any>) -> Unit
    ) = adapterDelegateViewBindingViewHolder.bind(bindingBlock)

    @ColorInt
    override fun getColor(
        @ColorRes id: Int
    ): Int = adapterDelegateViewBindingViewHolder.getColor(id)

    override fun getColorStateList(
        @ColorRes id: Int
    ): ColorStateList? = adapterDelegateViewBindingViewHolder.getColorStateList(id)

    override fun getDrawable(
        @DrawableRes id: Int
    ): Drawable? = adapterDelegateViewBindingViewHolder.getDrawable(id)

    override fun getString(
        @StringRes resId: Int
    ): String = adapterDelegateViewBindingViewHolder.getString(resId)

    override fun getString(
        @StringRes resId: Int,
        vararg formatArgs: Any
    ): String = adapterDelegateViewBindingViewHolder.getString(
        resId,
        formatArgs
    )

    override fun onFailedToRecycleView(
        block: () -> Boolean
    ) = adapterDelegateViewBindingViewHolder.onFailedToRecycleView(block)

    override fun onViewAttachedToWindow(
        block: () -> Unit
    ) = adapterDelegateViewBindingViewHolder.onViewAttachedToWindow(block)

    override fun onViewDetachedFromWindow(
        block: () -> Unit
    ) = adapterDelegateViewBindingViewHolder.onViewDetachedFromWindow(block)

    override fun onViewRecycled(
        block: () -> Unit
    ) = adapterDelegateViewBindingViewHolder.onViewRecycled(block)

}