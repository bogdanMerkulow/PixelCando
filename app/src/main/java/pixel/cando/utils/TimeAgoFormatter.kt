package pixel.cando.utils

import org.ocpsoft.prettytime.PrettyTime
import java.time.LocalDateTime
import java.util.Locale

class TimeAgoFormatter {

    private val prettyTime = PrettyTime()

    fun format(
        localDateTime: LocalDateTime
    ): String {
        val defaultLocal = Locale.getDefault()
        val localeToUse = when (defaultLocal.language) {
            "en" -> defaultLocal
            else -> Locale.US
        }
        prettyTime.locale = localeToUse
        return prettyTime.format(localDateTime)
    }
}