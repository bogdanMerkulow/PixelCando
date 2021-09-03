package pixel.cando.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun doOnGlobalMain(
    action: suspend () -> Unit
) = GlobalScope.launch(Dispatchers.Main) {
    action.invoke()
}