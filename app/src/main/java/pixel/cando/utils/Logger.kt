package pixel.cando.utils

import com.elvishew.xlog.XLog
import com.microsoft.appcenter.crashes.Crashes
import pixel.cando.BuildConfig

fun logError(throwable: Throwable) {
    if (BuildConfig.DEBUG) {
        XLog.e(
            "Error happened",
            throwable
        )
    } else {
        Crashes.trackError(throwable)
    }
}

fun logError(message: String) {
    if (BuildConfig.DEBUG.not()) {
        XLog.e(message)
    }
}