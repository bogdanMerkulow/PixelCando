package pixel.cando.utils

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

val Bitmap.base64: String?
    get() = try {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    } catch (t: Throwable) {
        logError(t)
        null
    }

private const val BITMAP_MAX_SIDE_SIZE = 960

val Bitmap.reduced: Bitmap
    get() {
        if (width <= BITMAP_MAX_SIDE_SIZE
            && height <= BITMAP_MAX_SIDE_SIZE
        ) {
            return this
        }
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = BITMAP_MAX_SIDE_SIZE
            newHeight = ((height.toFloat() / width.toFloat()) * BITMAP_MAX_SIDE_SIZE).toInt()
        } else {
            newHeight = BITMAP_MAX_SIDE_SIZE
            newWidth = ((width.toFloat() / height.toFloat()) * BITMAP_MAX_SIDE_SIZE).toInt()
        }
        return Bitmap.createScaledBitmap(
            this,
            newWidth,
            newHeight,
            false
        )
    }

val Bitmap.base64ForSending: String?
    get() = reduced.base64