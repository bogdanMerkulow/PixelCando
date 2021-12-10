package pixel.cando.data.models

import java.time.LocalDateTime

data class Photo(
    val id: Long,
    val imageUrl: String,
    val createdAt: LocalDateTime,
    val state: PhotoState,
    val note: String?,
)
