package pixel.cando.ui.main.camera

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pixel.cando.R
import pixel.cando.databinding.FragmentCameraBinding
import pixel.cando.ui._base.fragment.ViewBindingFullscreenDialogFragment
import pixel.cando.ui._base.fragment.findImplementation
import pixel.cando.utils.context
import pixel.cando.utils.dpToPx
import pixel.cando.utils.gone
import pixel.cando.utils.logError
import pixel.cando.utils.visibleOrGone
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

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

    private var lensFacing = CameraSelector.LENS_FACING_BACK
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
        viewBinding.cameraSwitcher.gone()

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

        viewBinding.cameraSwitcher.setOnClickListener {
            lensFacing = when (lensFacing) {
                CameraSelector.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_BACK
                CameraSelector.LENS_FACING_BACK -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Illegal lensFacing value")
            }
            startCamera(viewBinding)
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

                viewBinding.cameraSwitcher.visibleOrGone(
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                            && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                )

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewBinding.cameraView.surfaceProvider)
                    }

                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

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

        val context = requireContext()
        imageCapture?.let { imageCapture ->
            imageCapture.flashMode = flashMode

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                },
                ContentValues()
                    .apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "${UUID.randomUUID()}.jpeg")
                    }
            ).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        logError(exception)
                        Toast.makeText(
                            context,
                            getString(R.string.something_went_wrong),
                            Toast.LENGTH_LONG
                        ).show()

                        refreshAccordingToRotationCheckResult()
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        lifecycleScope.launch(Dispatchers.Default) {
                            val uri = getPortraitBitmap(
                                context,
                                outputFileResults.savedUri!!
                            )
                            withContext(Dispatchers.Main) {
                                dismiss()
                                findImplementation<Callback>()?.onCameraResult(uri)
                            }
                        }
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
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.camera_help_link))
                )
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .create()
            .show()
    }

    interface Callback {
        fun onCameraResult(uri: Uri)
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

@WorkerThread
private fun getPortraitBitmap(
    context: Context,
    uri: Uri
): Uri {

    return try {

        val bitmapGetter = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(context.contentResolver, uri)
                )
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        }

        val stream = context.contentResolver.openInputStream(uri)
            ?: return uri

        val orientation = ExifInterface(
            stream
        ).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                val bitmap = bitmapGetter.invoke()
                if (bitmap.width > bitmap.height) rotate(bitmap, 90) else bitmap
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                val bitmap = bitmapGetter.invoke()
                rotate(bitmap, 180)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                val bitmap = bitmapGetter.invoke()
                if (bitmap.width > bitmap.height) rotate(bitmap, 270) else bitmap
            }
            else -> return uri
        }

        val photoFile = File(
            context.cacheDir.absolutePath,
            "${UUID.randomUUID()}.jpeg"
        )

        FileOutputStream(photoFile).use {
            rotatedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                100,
                it
            )
        }

        photoFile.toUri()

    } catch (ex: IOException) {
        logError(ex)
        uri
    }
}

private fun rotate(bitmap: Bitmap, degree: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height

    val matrix = Matrix()
    matrix.postRotate(degree.toFloat())

    return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
}