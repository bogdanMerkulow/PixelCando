package pixel.cando.ui._base.tea

import com.spotify.mobius.EventSource
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


interface ResultEmitter<R> {
    fun emit(
        result: R
    )
}

class ResultEventSource<R, E>(
    private val mapper: (R) -> E
) : EventSource<E>,
    ResultEmitter<R>,
    CoroutineScope {

    private val parentJob = SupervisorJob()

    override val coroutineContext: CoroutineContext = parentJob + Dispatchers.Default

    private val channel = Channel<R>()

    override fun subscribe(
        eventConsumer: Consumer<E>
    ): Disposable {
        val job = launch {
            for (event in channel) {
                eventConsumer.accept(
                    mapper.invoke(event)
                )
            }
        }
        return Disposable {
            job.cancel()
        }
    }

    override fun emit(
        result: R
    ) {
        launch {
            channel.send(result)
        }
    }

}

fun ResultEmitter<Unit>.emit() = emit(Unit)