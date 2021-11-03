package pixel.cando.utils

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import pixel.cando.R
import java.util.Locale

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

    fun getCurrentLocale(): Locale

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

    override fun getCurrentLocale(
    ): Locale = Locale(
        context.getString(R.string.locale_language),
        context.getString(R.string.locale_country),
    )
}