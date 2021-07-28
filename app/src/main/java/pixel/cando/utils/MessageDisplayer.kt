package pixel.cando.utils

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

interface MessageDisplayer {
    fun showMessage(message: String)
}

val Fragment.messageDisplayer: MessageDisplayer
    get() = object : MessageDisplayer {

        private val mainHandler = Handler(Looper.getMainLooper())

        override fun showMessage(message: String) {
            val action = {
                Snackbar.make(
                    requireView(),
                    message,
                    Snackbar.LENGTH_LONG
                ).show()
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action()
            } else {
                mainHandler.post(
                    action
                )
            }
        }
    }