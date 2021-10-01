package pixel.cando.ui.main.camera

import android.app.AlertDialog
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentManager
import pixel.cando.R
import pixel.cando.databinding.FragmentCameraBinding
import pixel.cando.ui._base.fragment.ViewBindingFullscreenDialogFragment
import pixel.cando.ui._base.fragment.findImplementation
import pixel.cando.utils.context
import pixel.cando.utils.dpToPx
import pixel.cando.utils.logError
import java.io.File
import java.io.IOException
import java.util.*

class CameraFragment : ViewBindingFullscreenDialogFragment<FragmentCameraBinding>(
    FragmentCameraBinding::inflate
) {

    companion object {
        fun show(
            fragmentManager: FragmentManager
        ) {
            CameraFragment().show(
                fragmentManager, ""
            )
        }
    }

    private val deviceRotationChecker by lazy {
        DeviceRotationChecker(
            requireContext()
        ) { result ->
            if (result == DeviceRotationChecker.Result.NOT_AVAILABLE
                && result != rotationCheckResult
            ) {
                showInstructionsDialog()
            }
            rotationCheckResult = result
            refreshAccordingToRotationCheckResult()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTimerActive = false
    private var rotationCheckResult = DeviceRotationChecker.Result.OK

    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var isTimerEnabled = false
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentCameraBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        initViews(viewBinding)
        startCamera(viewBinding)
    }

    override fun onStart() {
        super.onStart()
        deviceRotationChecker.start()
    }

    override fun onStop() {
        super.onStop()
        deviceRotationChecker.stop()
        if (isTimerActive) {
            isTimerActive = false
            refreshAccordingToRotationCheckResult()
        }
    }

    private fun initViews(
        viewBinding: FragmentCameraBinding
    ) {
        val cameraOverlap = CameraOverlapView(requireContext())
        viewBinding.cameraContainerView.addView(
            cameraOverlap,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        viewBinding.flashSwitcher.setOnClickListener {
            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> {
                    flashMode = ImageCapture.FLASH_MODE_ON
                    viewBinding.flashSwitcher.setImageResource(R.drawable.ic_flash_on)
                }
                ImageCapture.FLASH_MODE_ON -> {
                    flashMode = ImageCapture.FLASH_MODE_AUTO
                    viewBinding.flashSwitcher.setImageResource(R.drawable.ic_flash_auto)
                }
                ImageCapture.FLASH_MODE_AUTO -> {
                    flashMode = ImageCapture.FLASH_MODE_OFF
                    viewBinding.flashSwitcher.setImageResource(R.drawable.ic_flash_off)
                }
            }
        }

        viewBinding.timerSwitcher.setOnClickListener {
            isTimerEnabled = !isTimerEnabled
            viewBinding.timerSwitcher.setImageResource(
                if (isTimerEnabled) R.drawable.ic_timer_on
                else R.drawable.ic_timer_off
            )
        }

        viewBinding.takePhoto.setOnClickListener {
            if (isTimerEnabled) {
                takePhotoWithTimer()
            } else {
                takePhoto()
            }
        }

        viewBinding.cancel.setOnClickListener {
            dismiss()
            findImplementation<Callback>()?.onCameraCancel()
        }
    }

    private fun startCamera(
        viewBinding: FragmentCameraBinding
    ) {

        val context = viewBinding.context
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewBinding.cameraView.surfaceProvider)
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val imageCapture = ImageCapture.Builder()
                        .build()
                    val camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    this.imageCapture = imageCapture
                    this.camera = camera

                } catch (ex: Exception) {
                    logError(ex)
                    Toast.makeText(
                        context,
                        getString(R.string.something_went_wrong),
                        Toast.LENGTH_LONG
                    ).show()
                }

            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun refreshAccordingToRotationCheckResult() {
        if (isTimerActive) {
            if (rotationCheckResult != DeviceRotationChecker.Result.OK) {
                isTimerActive = false
            }
        }
        viewBinding?.apply {
            when (rotationCheckResult) {
                DeviceRotationChecker.Result.OK -> {
                    rotationStateDescription.setText(R.string.camera_device_position_ok)
                    rotationStateImage.setImageResource(R.drawable.ic_rotate_ok)
                    setTakeButtonEnabled(true)
                }
                DeviceRotationChecker.Result.NEED_ROTATE_LEFT -> {
                    rotationStateDescription.setText(R.string.camera_rotate_device_left)
                    rotationStateImage.setImageResource(R.drawable.ic_rotate_left)
                    setTakeButtonEnabled(false)
                }
                DeviceRotationChecker.Result.NEED_ROTATE_RIGHT -> {
                    rotationStateDescription.setText(R.string.camera_rotate_device_right)
                    rotationStateImage.setImageResource(R.drawable.ic_rotate_right)
                    setTakeButtonEnabled(false)
                }
                DeviceRotationChecker.Result.NEED_ROTATE_SCREEN_UPWARDS -> {
                    rotationStateDescription.setText(R.string.camera_move_devices_top_away_from_you)
                    rotationStateImage.setImageResource(R.drawable.ic_rotate_from)
                    setTakeButtonEnabled(false)
                }
                DeviceRotationChecker.Result.NEED_ROTATE_SCREEN_DOWNWARDS -> {
                    rotationStateDescription.setText(R.string.camera_move_devices_top_closer_to_you)
                    rotationStateImage.setImageResource(R.drawable.ic_rotate_to)
                    setTakeButtonEnabled(false)
                }
                DeviceRotationChecker.Result.NOT_AVAILABLE -> {
                    rotationStateDescription.text = null
                    rotationStateImage.setImageDrawable(null)
                    setTakeButtonEnabled(true)
                }
            }
        }
    }

    private fun setTakeButtonEnabled(
        enabled: Boolean
    ) {
        viewBinding?.apply {
            if (takePhoto.isEnabled != enabled) {
                takePhoto.setImageResource(
                    if (enabled) R.drawable.ic_camera_button_on
                    else R.drawable.ic_camera_button_off
                )
                takePhoto.isEnabled = enabled
            }
        }
    }

    private fun takePhoto() {
        setTakeButtonEnabled(false)

        imageCapture?.let { imageCapture ->
            imageCapture.flashMode = flashMode

            val photoFile = File(
                requireContext().cacheDir.absolutePath,
                "${UUID.randomUUID()}.jpeg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        logError(exception)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.something_went_wrong),
                            Toast.LENGTH_LONG
                        ).show()

                        refreshAccordingToRotationCheckResult()
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val bitmap = getPortraitBitmap(
                            requireContext(),
                            photoFile.toUri()
                        )
                        photoFile.delete()
                        dismiss()
                        findImplementation<Callback>()?.onCameraResult(bitmap)
                    }
                })
        }
    }

    private fun takePhotoWithTimer() {
        setTakeButtonEnabled(false)
        val counter = 10
        val refreshCounterView = { count: Int ->
            viewBinding?.rotationStateDescription?.text = resources.getQuantityString(
                R.plurals.camera_seconds_remain,
                count,
                count
            )
        }
        refreshCounterView.invoke(counter)

        val period = 1000L

        fun delayedRecursive(
            counter: Int
        ) {
            mainHandler.postDelayed(
                {
                    if (isTimerActive) {
                        when (counter) {
                            1, 2, 3 -> {
                                refreshCounterView.invoke(counter)
                                showFlash()
                                mainHandler.postDelayed(
                                    {
                                        if (isTimerActive) {
                                            showFlash()
                                        }
                                    },
                                    333L
                                )
                                mainHandler.postDelayed(
                                    {
                                        if (isTimerActive) {
                                            showFlash()
                                        }
                                    },
                                    666L
                                )
                            }
                            0 -> {
                                isTimerActive = false
                                viewBinding?.rotationStateDescription?.text = null
                                takePhoto()
                            }
                            else -> {
                                refreshCounterView.invoke(counter)
                                showFlash()
                            }
                        }
                        if (isTimerActive) {
                            delayedRecursive(counter - 1)
                        }
                    }
                },
                period
            )
        }

        isTimerActive = true
        delayedRecursive(counter - 1)

    }

    private fun showFlash() {
        val camera = this.camera ?: return
        camera.cameraControl.enableTorch(true)
        mainHandler.postDelayed(
            {
                camera.cameraControl.enableTorch(false)
            },
            100L
        )
    }

    private fun showInstructionsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.camera_rotation_check_not_available_title)
            .setMessage(R.string.camera_rotation_check_not_available_message)
            .setNeutralButton(android.R.string.ok) { _, _ -> }
            .create()
            .show()
    }

    interface Callback {
        fun onCameraResult(bitmap: Bitmap)
        fun onCameraCancel()
    }

}

private class CameraOverlapView : View {

    private val redPaint by lazy(LazyThreadSafetyMode.NONE) {
        val paint = Paint()
        paint.color = Color.argb((255 * 0.95).toInt(), 255, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = lineWidth
        paint
    }

    private val whitePaint by lazy(LazyThreadSafetyMode.NONE) {
        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = lineWidth
        paint
    }

    private val lineWidth = dpToPx(4f)
    private val verticalPadding = dpToPx(30f)
    private val horizontalPadding = dpToPx(20f)
    private val shadowOffset = dpToPx(2f)

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(
        canvas: Canvas
    ) {
        // white lines
        canvas.drawLine(
            horizontalPadding + shadowOffset,
            verticalPadding + lineWidth / 2f,
            width - horizontalPadding + shadowOffset,
            verticalPadding + lineWidth / 2f,
            whitePaint
        )
        canvas.drawLine(
            width / 2f + lineWidth / 2f,
            verticalPadding,
            width / 2f + lineWidth / 2f,
            height - verticalPadding,
            whitePaint
        )
        canvas.drawLine(
            horizontalPadding + shadowOffset,
            height - verticalPadding + lineWidth / 2f,
            width - horizontalPadding + shadowOffset,
            height - verticalPadding + lineWidth / 2f,
            whitePaint
        )
        // red lines
        canvas.drawLine(
            horizontalPadding,
            verticalPadding,
            width - horizontalPadding,
            verticalPadding,
            redPaint
        )
        canvas.drawLine(
            width / 2f,
            verticalPadding,
            width / 2f,
            height - verticalPadding,
            redPaint
        )
        canvas.drawLine(
            horizontalPadding,
            height - verticalPadding,
            width - horizontalPadding,
            height - verticalPadding,
            redPaint
        )
    }

}

private class DeviceRotationChecker(
    context: Context,
    private val listener: (Result) -> Unit
) {

    private val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager

    private val rotationSensor = sensorManager.getDefaultSensor(
        Sensor.TYPE_ROTATION_VECTOR
    )

    private val rotationListener = object : SensorEventListener {

        private var lastResult: Result? = null

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int
        ) {
        }

        override fun onSensorChanged(
            event: SensorEvent
        ) {
            val rotationMatrix = FloatArray(16) { 0f }
            SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                event.values
            )
            val remappedRotationMatrix = FloatArray(16) { 0f }
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedRotationMatrix
            )
            val orientations = FloatArray(3) { 0f }
            SensorManager.getOrientation(remappedRotationMatrix, orientations)
            for (i in 0 until 3) {
                orientations[i] = Math.toDegrees(orientations[i].toDouble()).toFloat()
            }
            val newResult = when {
                orientations[2] < -10 -> Result.NEED_ROTATE_RIGHT
                orientations[2] > 10 -> Result.NEED_ROTATE_LEFT
                orientations[1] < -10 -> Result.NEED_ROTATE_SCREEN_UPWARDS
                orientations[1] > 10 -> Result.NEED_ROTATE_SCREEN_DOWNWARDS
                else -> Result.OK
            }
            if (lastResult != newResult) {
                lastResult = newResult
                listener(newResult)
            }
        }
    }

    fun start() {
        val isRegistered = sensorManager.registerListener(
            rotationListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        if (isRegistered.not()) {
            listener.invoke(Result.NOT_AVAILABLE)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(rotationListener)
    }

    enum class Result {
        OK,
        NEED_ROTATE_LEFT,
        NEED_ROTATE_RIGHT,
        NEED_ROTATE_SCREEN_UPWARDS,
        NEED_ROTATE_SCREEN_DOWNWARDS,
        NOT_AVAILABLE
    }

}

private fun getPortraitBitmap(
    context: Context,
    uri: Uri
): Bitmap {

    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, uri)
        )
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

    return try {

        val stream = context.contentResolver.openInputStream(uri) ?: return bitmap

        val orientation = ExifInterface(
            stream
        ).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                if (bitmap.width > bitmap.height) rotate(bitmap, 90) else bitmap
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate(bitmap, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                if (bitmap.width > bitmap.height) rotate(bitmap, 270) else bitmap
            }
            else -> bitmap
        }

    } catch (ex: IOException) {
        logError(ex)
        bitmap
    }
}

private fun rotate(bitmap: Bitmap, degree: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height

    val matrix = Matrix()
    matrix.postRotate(degree.toFloat())

    return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
}