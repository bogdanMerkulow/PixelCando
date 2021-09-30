package pixel.cando.data.models

import java.time.LocalDateTime

data class Exam(
    val id: Long,
    val createdAt: LocalDateTime,
    val number: Int,
    val bmi: Float,
)