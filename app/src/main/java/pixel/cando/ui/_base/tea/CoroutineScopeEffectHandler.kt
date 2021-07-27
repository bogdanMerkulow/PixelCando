package pixel.cando.ui._base.tea

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.*
import pixel.cando.utils.logError
import kotlin.coroutines.CoroutineContext

class CoroutineScopeEffectHandler<F, E>(
    private val handler: suspend CoroutineScope.(value: F, Consumer<E>) -> Unit
) : Connectable<F, E>,
    CoroutineScope {

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logError(throwable)
        }

    override fun connect(
        output: Consumer<E>
    ): Connection<F> {
        return object : Connection<F> {

            override fun accept(value: F) {
                launch {
                    handler.invoke(
                        this,
                        value,
                        output
                    )
                }
            }

            override fun dispose() {
                coroutineContext.job.cancelChildren()
            }
        }
    }
}