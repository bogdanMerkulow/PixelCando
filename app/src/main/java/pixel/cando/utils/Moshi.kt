package pixel.cando.utils

import com.squareup.moshi.Moshi

internal inline fun <reified T> Moshi.objectToJson(
    obj: T
) = this
    .adapter(T::class.java)
    .toJson(obj)

internal inline fun <reified T> Moshi.objectFromJson(
    json: String
) = this
    .adapter(T::class.java)
    .fromJson(json)
