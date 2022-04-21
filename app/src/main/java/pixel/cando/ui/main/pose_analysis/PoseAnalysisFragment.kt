package pixel.cando.ui.main.pose_analysis

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.fragment.app.FragmentManager
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import pixel.cando.databinding.FragmentPoseAnalysisBinding
import pixel.cando.ui._base.fragment.OnBackPressedListener
import pixel.cando.ui._base.fragment.ViewBindingFullscreenDialogFragment
import pixel.cando.ui._base.tea.EventSender
import pixel.cando.ui._base.tea.EventSenderNeeder
import pixel.cando.ui._base.tea.ViewModelRender
import pixel.cando.utils.diffuser.Diffuser
import pixel.cando.utils.diffuser.Diffuser.into
import pixel.cando.utils.diffuser.Diffuser.map
import pixel.cando.utils.diffuser.DiffuserCreator
import pixel.cando.utils.diffuser.DiffuserProvider
import pixel.cando.utils.diffuser.DiffuserProviderNeeder
import pixel.cando.utils.mlkit.PoseDetectorProcessor
import pixel.cando.utils.mlkit.PoseGraphic

class PoseAnalysisFragment : ViewBindingFullscreenDialogFragment<FragmentPoseAnalysisBinding>(
    FragmentPoseAnalysisBinding::inflate
), ViewModelRender<PoseAnalysisViewModel>,
    EventSenderNeeder<PoseAnalysisEvent>,
    DiffuserCreator<PoseAnalysisViewModel, FragmentPoseAnalysisBinding>,
    DiffuserProviderNeeder<PoseAnalysisViewModel>,
    OnBackPressedListener {

    override var eventSender: EventSender<PoseAnalysisEvent>? = null

    override var diffuserProvider: DiffuserProvider<PoseAnalysisViewModel>? = null

    private val poseDetectorProcessor by lazy {
        PoseDetectorProcessor(
            options = AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build(),
            showInFrameLikelihood = false,
            visualizeZ = false,
            rescaleZForVisualization = false,
        )
    }

    override fun createDiffuser(
        viewBinding: FragmentPoseAnalysisBinding
    ): Diffuser<PoseAnalysisViewModel> {
        return Diffuser(
            map(
                { it.uri },
                into {
                    viewBinding.photoImageView.setImageURI(it)
                    val bitmap = (viewBinding.photoImageView.drawable as BitmapDrawable).bitmap

                    viewBinding.photoOverlayView.setImageSourceInfo(
                        bitmap.width, bitmap.height, false
                    )
                    poseDetectorProcessor.processBitmap(
                        bitmap,
                        viewBinding.photoOverlayView
                    )

//                    viewBinding.root.viewTreeObserver.addOnGlobalLayoutListener(
//                        object : ViewTreeObserver.OnGlobalLayoutListener {
//                            override fun onGlobalLayout() {
//                                viewBinding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
//
//                                val scaleFactor = originalBitmap.height.toFloat() / viewBinding.photoImageView.height.toFloat()
//                                val resizedBitmap = Bitmap.createScaledBitmap(
//                                    originalBitmap,
//                                    (originalBitmap.width / scaleFactor).toInt(),
//                                    (originalBitmap.height / scaleFactor).toInt(),
//                                    true
//                                )
//
//                                viewBinding.photoOverlayView.setImageSourceInfo(
//                                    resizedBitmap.width, resizedBitmap.height, false
//                                )
//                                poseDetectorProcessor.processBitmap(
//                                    resizedBitmap,
//                                    viewBinding.photoOverlayView
//                                )
//                            }
//                        }
//                    )
                }
            ),
            map(
                { it.message },
                into {
                    viewBinding.messageLabel.text = it
                }
            )
        )
    }

    override fun onViewBindingCreated(
        viewBinding: FragmentPoseAnalysisBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        viewBinding.continueButton.setOnClickListener {
            eventSender?.sendEvent(
                PoseAnalysisEvent.ConfirmTap
            )
        }
        viewBinding.cancelButton.setOnClickListener {
            eventSender?.sendEvent(
                PoseAnalysisEvent.CancelTap
            )
        }
    }

    override fun renderViewModel(
        viewModel: PoseAnalysisViewModel
    ) {
        diffuserProvider?.invoke()?.run(viewModel)
    }

    override fun onBackPressed() {
        eventSender?.sendEvent(
            PoseAnalysisEvent.BackTap
        )
    }
}