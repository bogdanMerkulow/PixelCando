package pixel.cando.utils

import android.content.Context
import android.graphics.PointF
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import pixel.cando.R
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.sqrt

class PoseChecker(
    private val context: Context
) {

    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
    )

    suspend fun check(
        uri: Uri
    ) = suspendCoroutine<PoseCheckerResult> { continuation ->
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            continuation.resumeWithException(e)
            return@suspendCoroutine
        }
        val task = poseDetector.process(image)
            .addOnSuccessListener { pose ->
                if (pose.allPoseLandmarks.isEmpty()) {
                    continuation.resume(
                        PoseCheckerResult(
                            success = false,
                            message = context.getString(R.string.pose_detection_failed)
                        )
                    )
                } else {
                    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)

                    if (rightShoulder != null
                        && leftShoulder != null
                    ) {

                        val leftSide =
                            rightShoulder.position3D.z - leftShoulder.position3D.z > 0f
                        val shoulder = if (leftSide) leftShoulder
                        else rightShoulder
                        val eye = pose.getPoseLandmark(
                            if (leftSide) PoseLandmark.LEFT_EYE
                            else PoseLandmark.RIGHT_EYE
                        )
                        val ankle = pose.getPoseLandmark(
                            if (leftSide) PoseLandmark.LEFT_ANKLE
                            else PoseLandmark.RIGHT_ANKLE
                        )
                        val hip = pose.getPoseLandmark(
                            if (leftSide) PoseLandmark.LEFT_HIP
                            else PoseLandmark.RIGHT_HIP
                        )
                        val elbow = pose.getPoseLandmark(
                            if (leftSide) PoseLandmark.LEFT_ELBOW
                            else PoseLandmark.RIGHT_ELBOW
                        )
                        val wrist = pose.getPoseLandmark(
                            if (leftSide) PoseLandmark.LEFT_WRIST
                            else PoseLandmark.RIGHT_WRIST
                        )
                        val knee = pose.getPoseLandmark(
                            if (leftSide) PoseLandmark.LEFT_KNEE
                            else PoseLandmark.RIGHT_KNEE
                        )

                        if (eye == null
                            || ankle == null
                            || hip == null
                            || elbow == null
                            || wrist == null
                            || knee == null
                        ) {
                            continuation.resume(
                                PoseCheckerResult(
                                    success = false,
                                    message = context.getString(R.string.pose_detection_failed)
                                )
                            )

                        } else {
                            val height = Math.abs(shoulder.position.y - ankle.position.y)
                            val eyeInFrame = eye.inFrameLikelihood > 0.5f
                                    && eye.position.y > 0f
                                    && eye.position.y < image.height
                            val ankleInFrame = ankle.inFrameLikelihood > 0.5f
                                    && ankle.position.y > 0f
                                    && ankle.position.y < image.height
                            val distanceEyeAnkle = eye.position.distanceXY(ankle.position)
                            val distanceEyeHip = eye.position.distanceXY(hip.position)
                            val distanceHipAnkle = hip.position.distanceXY(ankle.position)
                            val difference =
                                Math.abs(distanceEyeAnkle - (distanceEyeHip + distanceHipAnkle))

                            if (eyeInFrame.not()
                                || ankleInFrame.not()
                                || difference > 100.0
                            ) {
                                continuation.resume(
                                    PoseCheckerResult(
                                        success = false,
                                        message = context.getString(R.string.pose_detection_missed_head_or_feet)
                                    )
                                )
                            } else if (
                                distanceEyeAnkle / image.height < 0.6
                            ) {
                                continuation.resume(
                                    PoseCheckerResult(
                                        success = false,
                                        message = context.getString(R.string.pose_detection_too_far_from_camera)
                                    )
                                )
                            } else {
                                val angleShoulderAnkle =
                                    angleBetweenVerticalAndLineThroughTwoPoints(
                                        center = shoulder.position,
                                        secondPoint = ankle.position
                                    )
                                val subjectIsVertical = angleShoulderAnkle > 170.0
                                val subjectIsNotUpsideDown =
                                    (ankle.position.y - eye.position.y) > 0
                                if (subjectIsVertical.not()) {
                                    continuation.resume(
                                        PoseCheckerResult(
                                            success = false,
                                            message = context.getString(R.string.pose_detection_not_vertical_position)
                                        )
                                    )
                                } else if (subjectIsNotUpsideDown.not()) {
                                    continuation.resume(
                                        PoseCheckerResult(
                                            success = false,
                                            message = context.getString(R.string.pose_detection_upside_down)
                                        )
                                    )
                                } else {
                                    val distanceElbowHipX =
                                        Math.abs(elbow.position.x - hip.position.x)
                                    val distanceWristHipX =
                                        Math.abs(wrist.position.x - hip.position.x)
                                    val elbowAngle = angleBetweenThreePoints(
                                        center = elbow.position,
                                        firstPoint = shoulder.position,
                                        secondPoint = wrist.position
                                    )
                                    val shoulderAngle = angleBetweenThreePoints(
                                        center = wrist.position,
                                        firstPoint = shoulder.position,
                                        secondPoint = knee.position
                                    )
                                    val angleDifference = Math.abs(shoulderAngle - elbowAngle)
                                    if (angleDifference < 6) {
                                        continuation.resume(
                                            PoseCheckerResult(
                                                success = true,
                                                message = null
                                            )
                                        )
                                    } else if (distanceWristHipX / height * 100f > 4f) {
                                        continuation.resume(
                                            PoseCheckerResult(
                                                success = false,
                                                message = context.getString(R.string.pose_detection_arm_not_vertical_wirst_misaligned)
                                            )
                                        )
                                    } else if (distanceElbowHipX / height * 100f > 4f) {
                                        continuation.resume(
                                            PoseCheckerResult(
                                                success = false,
                                                message = context.getString(R.string.pose_detection_arm_not_vertical_elbow_misaligned)
                                            )
                                        )
                                    } else {
                                        continuation.resume(
                                            PoseCheckerResult(
                                                success = false,
                                                message = context.getString(R.string.pose_detection_arm_not_vertical)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        continuation.resume(
                            PoseCheckerResult(
                                success = false,
                                message = context.getString(R.string.pose_detection_failed)
                            )
                        )
                    }
                }
            }
            .addOnFailureListener {
                logError(it)
                continuation.resume(
                    PoseCheckerResult(
                        success = false,
                        message = context.getString(R.string.pose_detection_failed)
                    )
                )
            }
    }

}

data class PoseCheckerResult(
    val success: Boolean,
    val message: String?,
)

private fun PointF.distanceXY(
    point: PointF
) = sqrt(
    Math.pow(Math.abs(x - point.x).toDouble(), 2.0) + Math.pow(
        Math.abs(y - point.y).toDouble(), 2.0
    )
)

private fun angleBetweenVerticalAndLineThroughTwoPoints(
    center: PointF,
    secondPoint: PointF
) = angleBetweenThreePoints(
    center = center,
    firstPoint = PointF(
        center.x,
        center.y - 200f,
    ),
    secondPoint = secondPoint,
)

private fun angleBetweenThreePoints(
    center: PointF,
    firstPoint: PointF,
    secondPoint: PointF
): Double {
    val p1X = center.x
    val p2X = firstPoint.x
    val p3X = secondPoint.x
    val p1Y = center.y
    val p2Y = firstPoint.y
    val p3Y = secondPoint.y

    val numerator = p2Y * (p1X - p3X) + p1Y * (p3X - p2X) + p3Y * (p2X - p1X)
    val denominator = (p2X - p1X) * (p1X - p3X) + (p2Y - p1Y) * (p1Y - p3Y)
    val ratio = numerator / denominator

    val angleRad = Math.atan(ratio.toDouble())
    val angleDeg = angleRad * 180.0 / Math.PI

    return if (angleDeg < 0.0) {
        180.0 + angleDeg
    } else if (angleDeg < 90.0) {
        180.0 - angleDeg
    } else angleDeg
}