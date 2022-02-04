package pixel.cando.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.ceil

val Bitmap.base64: String
    get() {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

private const val BITMAP_MAX_SIDE_SIZE = 2048

fun Context.loadReducedBitmap(
    uri: Uri
): Bitmap {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    val fileDescriptor = contentResolver.openFileDescriptor(uri, "r")!!.fileDescriptor
    BitmapFactory.decodeFileDescriptor(
        fileDescriptor,
        null,
        options
    )
    val imageWidth = options.outWidth
    val imageHeight = options.outHeight

    if (imageWidth <= BITMAP_MAX_SIDE_SIZE
        && imageHeight <= BITMAP_MAX_SIDE_SIZE
    ) {
        return BitmapFactory.decodeFileDescriptor(
            fileDescriptor
        )
    }
    val ratio = ceil(
        maxOf(imageWidth, imageHeight).toDouble() / BITMAP_MAX_SIDE_SIZE.toDouble()
    ).toInt()
    return BitmapFactory.decodeFileDescriptor(
        fileDescriptor,
        null,
        BitmapFactory.Options().apply {
            inSampleSize = ratio
        }
    )
}