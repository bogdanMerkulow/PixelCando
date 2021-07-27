package pixel.cando.utils

import com.elvishew.xlog.XLog

fun logError(throwable: Throwable) {
    XLog.e(
        "Error happened",
        throwable
    )
}

fun logError(message: String) {
    XLog.e(message)
}