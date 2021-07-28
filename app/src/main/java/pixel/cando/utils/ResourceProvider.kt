package pixel.cando.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.*
import androidx.core.content.ContextCompat

interface ResourceProvider {

    fun getString(@StringRes id: Int): String

    fun getString(
        @StringRes id: Int,
        vararg args: Any
    ): String

    fun getDrawable(@DrawableRes id: Int): Drawable

    fun getColor(@ColorRes colorId: Int): Int

    fun getQuantityString(
        @PluralsRes id: Int,
        count: Int
    ): String

    fun getDimension(@DimenRes id: Int): Float

}

class RealResourceProvider(
    private val context: Context
) : ResourceProvider {

    override fun getString(@StringRes id: Int): String = context.getString(id)

    override fun getString(
        @StringRes id: Int,
        vararg args: Any
    ): String = context.getString(
        id,
        *args
    )

    override fun getDrawable(@DrawableRes id: Int): Drawable = ContextCompat.getDrawable(
        context,
        id
    )!!

    override fun getColor(@ColorRes colorId: Int) = ContextCompat.getColor(
        context,
        colorId
    )

    override fun getQuantityString(
        @PluralsRes id: Int,
        count: Int
    ): String =
        context.resources.getQuantityString(
            id,
            count,
            count
        )

    override fun getDimension(@DimenRes id: Int): Float = context.resources.getDimension(id)
}