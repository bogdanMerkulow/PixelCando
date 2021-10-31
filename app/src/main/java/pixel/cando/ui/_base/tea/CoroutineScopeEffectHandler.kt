package pixel.cando.ui._base.tea

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import pixel.cando.utils.logError
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class CoroutineScopeEffectHandler<F, E>(
    private val handler: suspend CoroutineScope.(value: F, Consumer<E>) -> Unit
) : Connectable<F, E>,
    CoroutineScope {

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logError(throwable)
        }

    private val isConnected = AtomicBoolean(false)

    override fun connect(
        output: Consumer<E>
    ): Connection<F> {

        isConnected.set(true)

        return object : Connection<F> {

            override fun accept(value: F) {
                launch {
                    handler.invoke(
                        this,
                        value,
                        {
                            if (isConnected.get()) {
                                output.accept(it)
                            }
                        }
                    )
                }
            }

            override fun dispose() {
                isConnected.set(false)
                coroutineContext.job.cancelChildren()
            }
        }
    }
}