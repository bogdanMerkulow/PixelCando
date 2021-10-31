package pixel.cando.ui._base.tea

import com.spotify.mobius.First
import com.spotify.mobius.Next

val <M, F> Next<M, F>.toFirst: First<M, F>
    get() = First.first(this.modelUnsafe(), this.effects())