package pixel.cando.ui._base.tea

import com.spotify.mobius.EventSource
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.disposables.Disposable

fun <M, E, F> MobiusLoop.Builder<M, E, F>.eventSources(
    vararg eventSources: EventSource<E>
): MobiusLoop.Builder<M, E, F> {
    val emptyEventSource = EventSource<E> {
        Disposable { }
    }
    return this.eventSources(
        emptyEventSource,
        *eventSources
    )
}