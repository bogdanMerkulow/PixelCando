package pixel.cando.utils

import kotlinx.coroutines.CancellationException

inline fun <T : Throwable, R> T.handleSkippingCancellation(
    block: () -> R
): R = if (this !is CancellationException) block()
else throw this