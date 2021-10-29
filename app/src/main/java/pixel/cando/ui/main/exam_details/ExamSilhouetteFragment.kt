package pixel.cando.ui.main.exam_details

import android.os.Bundle
import coil.load
import coil.request.Disposable
import pixel.cando.databinding.FragmentExamSilhouetteBinding
import pixel.cando.ui._base.fragment.ViewBindingFragment
import pixel.cando.utils.gone
import pixel.cando.utils.visible

class ExamSilhouetteFragment : ViewBindingFragment<FragmentExamSilhouetteBinding>(
    FragmentExamSilhouetteBinding::inflate
) {

    companion object {
        private const val ARG_SILHOUETTE_URL = "ARG_SILHOUETTE_URL"
        fun newInstance(
            silhouetteUrl: String
        ): ExamSilhouetteFragment {
            val args = Bundle()
            args.putString(ARG_SILHOUETTE_URL, silhouetteUrl)
            val fragment = ExamSilhouetteFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var imageLoadDisposable: Disposable? = null

    override fun onViewBindingCreated(
        viewBinding: FragmentExamSilhouetteBinding,
        savedInstanceState: Bundle?
    ) {
        super.onViewBindingCreated(viewBinding, savedInstanceState)
        imageLoadDisposable = viewBinding.silhouetteImageView.load(
            requireArguments().getString(ARG_SILHOUETTE_URL)
        ) {
            listener(
                onStart = {
                    viewBinding.progressBar.visible()
                },
                onCancel = {
                    viewBinding.progressBar.gone()
                },
                onError = { _, _ ->
                    viewBinding.progressBar.gone()
                },
                onSuccess = { _, _ ->
                    viewBinding.progressBar.gone()
                }
            )
        }
    }

    override fun onViewBindingDestroyed() {
        super.onViewBindingDestroyed()
        imageLoadDisposable?.dispose()
        imageLoadDisposable = null
    }

}