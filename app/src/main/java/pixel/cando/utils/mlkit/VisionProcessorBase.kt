package pixel.cando.utils.mlkit

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.android.gms.tasks.Tasks
import com.google.android.odml.image.BitmapMlImageBuilder
import com.google.android.odml.image.MlImage
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import pixel.cando.utils.logError

abstract class VisionProcessorBase<T> : VisionImageProcessor {

    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    override fun processBitmap(bitmap: Bitmap, graphicOverlay: GraphicOverlay) {
        if (isMlImageEnabled(graphicOverlay.context)) {
            val mlImage = BitmapMlImageBuilder(bitmap).build()
            requestDetectInImage(
                mlImage,
                graphicOverlay,
            )
            mlImage.close()
            return
        }

        requestDetectInImage(
            InputImage.fromBitmap(bitmap, 0),
            graphicOverlay,
        )
    }

    private fun requestDetectInImage(
        image: InputImage,
        graphicOverlay: GraphicOverlay,
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            graphicOverlay,
        )
    }

    private fun requestDetectInImage(
        image: MlImage,
        graphicOverlay: GraphicOverlay,
    ): Task<T> {
        return setUpListener(
            detectInImage(image),
            graphicOverlay,
        )
    }

    private fun setUpListener(
        task: Task<T>,
        graphicOverlay: GraphicOverlay,
    ): Task<T> {
        return task
            .addOnSuccessListener(
                executor
            ) { results: T ->
                graphicOverlay.clear()
                onSuccess(results, graphicOverlay)
                graphicOverlay.postInvalidate()
            }
            .addOnFailureListener(
                executor
            ) { e ->
                graphicOverlay.clear()
                graphicOverlay.postInvalidate()
                logError(e)
                onFailure(e)
            }
    }

    override fun stop() {
        executor.shutdown()
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected open fun detectInImage(image: MlImage): Task<T> {
        return Tasks.forException(
            MlKitException(
                "MlImage is currently not demonstrated for this feature",
                MlKitException.INVALID_ARGUMENT
            )
        )
    }

    protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)

    protected abstract fun onFailure(e: Exception)

    protected open fun isMlImageEnabled(context: Context?): Boolean {
        return false
    }
}
