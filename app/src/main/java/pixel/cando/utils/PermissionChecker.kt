package pixel.cando.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import pixel.cando.ui._base.fragment.SimpleFragmentDelegate
import pixel.cando.ui._base.tea.ResultEmitter
import pixel.cando.ui._base.tea.ResultEventSource

interface PermissionChecker {
    fun checkPermission(): Boolean

    fun requestPermission()
}

sealed class PermissionCheckerResult {
    object Granted : PermissionCheckerResult()
    object Denied : PermissionCheckerResult()
}

class RealPermissionChecker(
    private val permission: String,
    private val context: Context,
    private val resultEmitter: ResultEmitter<PermissionCheckerResult>
) : SimpleFragmentDelegate(),
    PermissionChecker {

    private var launcher: ActivityResultLauncher<String>? = null

    override fun requestPermission() {
        launcher?.launch(permission)
            ?: throw IllegalStateException("ActivityResultLauncher was not initialized")
    }

    override fun checkPermission(
    ): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onFragmentCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    ) {
        launcher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            resultEmitter.emit(
                if (it) PermissionCheckerResult.Granted
                else PermissionCheckerResult.Denied
            )
        }
    }

}

fun <E> createPermissionCheckerResultEventSource(
    mapper: (PermissionCheckerResult) -> E
) = ResultEventSource(mapper)