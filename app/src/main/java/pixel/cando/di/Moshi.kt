package pixel.cando.di

import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder

fun assembleMoshi() = Moshi.Builder()
    .add(LocalDateAdapter())
    .add(LocalDateTimeAdapter())
    .build()

private class LocalDateAdapter {

    @FromJson
    fun fromJson(
        localDate: String
    ): LocalDate {
        return LocalDate.parse(localDate)
    }

    @ToJson
    fun toJson(
        localDate: LocalDate
    ): String {
        return localDate.toString()
    }

}

private class LocalDateTimeAdapter {

    private val dateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .appendOffset(
            "+HH",
            ""
        )
        .toFormatter()

    @FromJson
    fun fromJson(
        localDateTime: String
    ): LocalDateTime {
        return LocalDateTime.parse(
            localDateTime,
            dateTimeFormatter
        )
    }

    @ToJson
    fun toJson(
        localDateTime: LocalDateTime
    ): String {
        return dateTimeFormatter.format(localDateTime)
    }

}