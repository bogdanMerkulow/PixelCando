package pixel.cando.utils.mlkit

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.odml.image.MlImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase
import pixel.cando.utils.logError
import java.util.concurrent.Executors

class PoseDetectorProcessor(
    options: PoseDetectorOptionsBase,
    private val showInFrameLikelihood: Boolean,
    private val visualizeZ: Boolean,
    private val rescaleZForVisualization: Boolean,
) : VisionProcessorBase<Pose>() {

    private val detector = PoseDetection.getClient(options)

    override fun stop() {
        super.stop()
        detector.close()
    }

    override fun detectInImage(image: InputImage): Task<Pose> {
        return detector
            .process(image)
    }

    override fun detectInImage(image: MlImage): Task<Pose> {
        return detector
            .process(image)
    }

    override fun onSuccess(
        results: Pose,
        graphicOverlay: GraphicOverlay
    ) {
        graphicOverlay.add(
            PoseGraphic(
                graphicOverlay,
                results,
                showInFrameLikelihood,
                visualizeZ,
                rescaleZForVisualization,
                emptyList()
            )
        )
    }

    override fun onFailure(e: Exception) {
        logError(e)
    }

    override fun isMlImageEnabled(context: Context?): Boolean {
        return true
    }

}