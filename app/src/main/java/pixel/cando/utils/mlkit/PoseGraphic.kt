package pixel.cando.utils.mlkit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import pixel.cando.utils.dpToPx
import pixel.cando.utils.mlkit.GraphicOverlay.Graphic

class PoseGraphic constructor(
    overlay: GraphicOverlay,
    private val pose: Pose,
) : Graphic(overlay) {

    private val dotRadius = overlay.dpToPx(2f)
    private val strokeWidth = overlay.dpToPx(2f)

    private val linePaint: Paint
    private val pointPaint: Paint

    init {
        pointPaint = Paint()
        pointPaint.strokeWidth = strokeWidth
        pointPaint.color = Color.BLUE
        linePaint = Paint()
        linePaint.strokeWidth = strokeWidth
        linePaint.color = Color.RED
    }

    override fun draw(canvas: Canvas) {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) {
            return
        }

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        val leftSide = if (rightShoulder != null
            && leftShoulder != null
        ) rightShoulder.position3D.z - leftShoulder.position3D.z > 0f
        else null

        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        val leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)
        val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER)
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)

        val rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER)
        val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        val rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER)
        val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
        val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)

        // Face
        if (leftSide == null
            || leftSide == true
        ) {
            drawLine(canvas, nose, leftEyeInner, linePaint)
            drawLine(canvas, leftEyeInner, leftEye, linePaint)
            drawLine(canvas, leftEye, leftEyeOuter, linePaint)
            drawLine(canvas, leftEyeOuter, leftEar, linePaint)
        }

        if (leftSide == null
            || leftSide == false
        ) {
            drawLine(canvas, nose, rightEyeInner, linePaint)
            drawLine(canvas, rightEyeInner, rightEye, linePaint)
            drawLine(canvas, rightEye, rightEyeOuter, linePaint)
            drawLine(canvas, rightEyeOuter, rightEar, linePaint)
        }

        // Left body
        if (leftSide == null
            || leftSide == true
        ) {
            drawLine(canvas, leftShoulder, leftElbow, linePaint)
            drawLine(canvas, leftElbow, leftWrist, linePaint)
            drawLine(canvas, leftShoulder, leftHip, linePaint)
            drawLine(canvas, leftHip, leftKnee, linePaint)
            drawLine(canvas, leftKnee, leftAnkle, linePaint)
            drawLine(canvas, leftWrist, leftThumb, linePaint)
            drawLine(canvas, leftWrist, leftPinky, linePaint)
            drawLine(canvas, leftWrist, leftIndex, linePaint)
            drawLine(canvas, leftIndex, leftPinky, linePaint)
            drawLine(canvas, leftAnkle, leftHeel, linePaint)
            drawLine(canvas, leftHeel, leftFootIndex, linePaint)
        }

        // Right body
        if (leftSide == null
            || leftSide == false
        ) {
            drawLine(canvas, rightShoulder, rightElbow, linePaint)
            drawLine(canvas, rightElbow, rightWrist, linePaint)
            drawLine(canvas, rightShoulder, rightHip, linePaint)
            drawLine(canvas, rightHip, rightKnee, linePaint)
            drawLine(canvas, rightKnee, rightAnkle, linePaint)
            drawLine(canvas, rightWrist, rightThumb, linePaint)
            drawLine(canvas, rightWrist, rightPinky, linePaint)
            drawLine(canvas, rightWrist, rightIndex, linePaint)
            drawLine(canvas, rightIndex, rightPinky, linePaint)
            drawLine(canvas, rightAnkle, rightHeel, linePaint)
            drawLine(canvas, rightHeel, rightFootIndex, linePaint)
        }

        if (leftSide == null
            || leftSide == true
        ) {
            drawPoint(canvas, leftEyeInner, pointPaint)
            drawPoint(canvas, leftEye, pointPaint)
            drawPoint(canvas, leftEyeOuter, pointPaint)
            drawPoint(canvas, leftEar, pointPaint)
            drawPoint(canvas, leftMouth, pointPaint)
            drawPoint(canvas, leftShoulder, pointPaint)
            drawPoint(canvas, leftElbow, pointPaint)
            drawPoint(canvas, leftWrist, pointPaint)
            drawPoint(canvas, leftHip, pointPaint)
            drawPoint(canvas, leftKnee, pointPaint)
            drawPoint(canvas, leftAnkle, pointPaint)
            drawPoint(canvas, leftPinky, pointPaint)
            drawPoint(canvas, leftIndex, pointPaint)
            drawPoint(canvas, leftThumb, pointPaint)
            drawPoint(canvas, leftHeel, pointPaint)
            drawPoint(canvas, leftFootIndex, pointPaint)
        }

        if (leftSide == null
            || leftSide == false
        ) {
            drawPoint(canvas, rightEyeInner, pointPaint)
            drawPoint(canvas, rightEye, pointPaint)
            drawPoint(canvas, rightEyeOuter, pointPaint)
            drawPoint(canvas, rightEar, pointPaint)
            drawPoint(canvas, rightMouth, pointPaint)
            drawPoint(canvas, rightShoulder, pointPaint)
            drawPoint(canvas, rightElbow, pointPaint)
            drawPoint(canvas, rightWrist, pointPaint)
            drawPoint(canvas, rightHip, pointPaint)
            drawPoint(canvas, rightKnee, pointPaint)
            drawPoint(canvas, rightAnkle, pointPaint)
            drawPoint(canvas, rightPinky, pointPaint)
            drawPoint(canvas, rightIndex, pointPaint)
            drawPoint(canvas, rightThumb, pointPaint)
            drawPoint(canvas, rightHeel, pointPaint)
            drawPoint(canvas, rightFootIndex, pointPaint)
        }

    }

    private fun drawPoint(canvas: Canvas, landmark: PoseLandmark?, paint: Paint) {
        val point = landmark?.position3D ?: return
        canvas.drawCircle(translateX(point.x), translateY(point.y), dotRadius, paint)
    }

    private fun drawLine(
        canvas: Canvas,
        startLandmark: PoseLandmark?,
        endLandmark: PoseLandmark?,
        paint: Paint
    ) {
        val start = startLandmark?.position3D ?: return
        val end = endLandmark?.position3D ?: return

        canvas.drawLine(
            translateX(start.x),
            translateY(start.y),
            translateX(end.x),
            translateY(end.y),
            paint
        )
    }

}