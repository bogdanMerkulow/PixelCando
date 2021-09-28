package pixel.cando.di

import pixel.cando.ui._base.fragment.FragmentDelegate
import pixel.cando.ui._base.tea.ResultEmitter
import pixel.cando.ui.main.photo_preview.PhotoPreviewFragment
import pixel.cando.ui.main.photo_preview.PhotoPreviewResult

fun PhotoPreviewFragment.setup() {
    val dependecies = this.findDelegateOrThrow<PhotoPreviewDependencies>()
    this.resultEmitter = dependecies.resultEmitter
}

interface PhotoPreviewDependencies : FragmentDelegate {
    val resultEmitter: ResultEmitter<PhotoPreviewResult>
}