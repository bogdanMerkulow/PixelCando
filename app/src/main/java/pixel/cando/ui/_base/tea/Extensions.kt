package pixel.cando.ui._base.tea

import com.spotify.mobius.EventSource
import com.spotify.mobius.First
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.Next
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

fun <M, F> Next<M, F>.toFirst(
    fallbackModel: M
): First<M, F> = First.first(
    this.modelOrElse(fallbackModel),
    this.effects()
)

fun <M, F> Next<M, F>.mapEffects(
    mapper: (Set<F>) -> Set<F>
): Next<M, F> {
    val newEffects = mapper.invoke(
        effects()
    )
    return if (hasModel()) Next.next(
        modelUnsafe(),
        newEffects
    ) else Next.dispatch(newEffects)
}