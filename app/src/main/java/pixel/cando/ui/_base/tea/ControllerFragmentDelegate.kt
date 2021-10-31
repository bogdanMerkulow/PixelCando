package pixel.cando.ui._base.tea

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.Init
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.extras.Connectables
import com.spotify.mobius.functions.Consumer
import pixel.cando.ui._base.fragment.SimpleFragmentDelegate

interface ViewModelRender<VM> {
    fun renderViewModel(viewModel: VM)
}

interface EventSender<E> {
    fun sendEvent(event: E)
}

interface EventSenderNeeder<E> {
    var eventSender: EventSender<E>?
}

class ControllerFragmentDelegate<VM, M : Parcelable, E, F>(
    private val loop: MobiusLoop.Builder<M, E, F>,
    private val initialState: Init<M, F>,
    private val defaultStateProvider: () -> M,
    private val modelMapper: (M) -> VM,
    private val render: ViewModelRender<VM>?
) : SimpleFragmentDelegate(),
    Connectable<VM, E>,
    EventSender<E> {

    private val keyModel = "KEY_MODEL"

    private var controller: MobiusLoop.Controller<M, E>? = null
    private var modelBeforeExit: M? = null

    private var eventConsumer: Consumer<E>? = null

    private fun retrieveDefaultDataModel(
        savedInstanceState: Bundle?
    ): M = modelBeforeExit
        ?: savedInstanceState?.getParcelable(keyModel)
        ?: defaultStateProvider.invoke()

    private fun getDefaultViewModel(
        savedInstanceState: Bundle?
    ): VM = modelMapper.invoke(
        retrieveDefaultDataModel(savedInstanceState)
    )

    override fun onFragmentCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    ) {
        modelBeforeExit = savedInstanceState?.getParcelable(keyModel)
    }

    override fun onFragmentViewCreated(
        fragment: Fragment,
        savedInstanceState: Bundle?
    ) {
        render?.renderViewModel(
            getDefaultViewModel(
                savedInstanceState
            )
        )
        val controller = this.controller ?: MobiusAndroid.controller(
            loop,
            retrieveDefaultDataModel(savedInstanceState),
            initialState
        ).also {
            this.controller = it
        }

        controller.connect(
            Connectables.contramap(
                { modelMapper.invoke(it) },
                this
            )
        )
    }

    override fun onFragmentResumed(
        fragment: Fragment
    ) {
        modelBeforeExit = null
        controller?.start()
    }

    override fun onFragmentPaused(
        fragment: Fragment
    ) {
        modelBeforeExit = controller?.model
        controller?.stop()
    }

    override fun onFragmentViewDestroyed(
        fragment: Fragment
    ) {
        controller?.disconnect()
    }

    override fun onFragmentSaveInstanceState(
        fragment: Fragment,
        outState: Bundle
    ) {
        outState.putParcelable(
            keyModel,
            controller?.model ?: modelBeforeExit
        )
    }

    override fun connect(output: Consumer<E>): Connection<VM> {

        eventConsumer = output

        return object : Connection<VM> {
            override fun accept(value: VM) {
                render?.renderViewModel(value)
            }

            override fun dispose() {
                eventConsumer = null
            }
        }
    }

    override fun sendEvent(
        event: E
    ) {
        eventConsumer?.accept(event)
    }

}