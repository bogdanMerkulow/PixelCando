package pixel.cando.ui

import pixel.cando.ui._base.tea.ResultEventSource
import pixel.cando.ui.root.RootEvent

fun createUnauthorizedResultEventSource() =
    ResultEventSource<RootEvent.UserAuthorizationGotInvalid, RootEvent> {
        it
    }