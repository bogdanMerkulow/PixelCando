package pixel.cando.utils

import org.ocpsoft.prettytime.PrettyTime
import java.time.LocalDateTime

class TimeAgoFormatter(
    private val resourceProvider: ResourceProvider
) {

    private val prettyTime = PrettyTime(
        resourceProvider.getCurrentLocale()
    )

    fun format(
        localDateTime: LocalDateTime
    ): String {
        return prettyTime.format(localDateTime)
    }
}