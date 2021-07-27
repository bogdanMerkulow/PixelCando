package pixel.cando.ui._base.fragment

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import java.io.Serializable

private const val argumentKey = "fragment_argument"

internal inline fun <reified ARG, F : Fragment> F.withArgumentSet(
    argument: ARG
) = this.apply {
    setArgument(argument)
}

internal inline fun <reified ARG> Fragment.setArgument(
    argument: ARG
) {
    val bundle = arguments ?: Bundle()
    when (argument) {
        is Int -> bundle.putInt(
            argumentKey,
            argument
        )
        is Long -> bundle.putLong(
            argumentKey,
            argument
        )
        is String -> bundle.putString(
            argumentKey,
            argument
        )
        is Parcelable -> bundle.putParcelable(
            argumentKey,
            argument
        )
        is Serializable -> bundle.putSerializable(
            argumentKey,
            argument
        )
        else -> throw IllegalArgumentException("Type ${ARG::class.java.name} is not supported")
    }
    arguments = bundle
}

internal inline fun <reified ARG> Fragment.getArgument(
): ARG {
    return arguments!!.let { bundle ->
        when {
            Int::class.java.isAssignableFrom(ARG::class.java) -> bundle.getInt(argumentKey) as ARG
            Long::class.java.isAssignableFrom(ARG::class.java) -> bundle.getLong(argumentKey) as ARG
            String::class.java.isAssignableFrom(ARG::class.java) -> bundle.getString(argumentKey) as ARG
            Parcelable::class.java.isAssignableFrom(ARG::class.java) -> bundle.getParcelable<Parcelable>(
                argumentKey
            ) as ARG
            Serializable::class.java.isAssignableFrom(ARG::class.java) -> bundle.getSerializable(
                argumentKey
            ) as ARG
            else -> throw IllegalArgumentException("Type ${ARG::class.java.name} is not supported")
        }
    }
}