package pixel.cando.ui

import pixel.cando.ui._base.tea.ResultEventSource

fun createUnauthorizedResultEventSource() =
    ResultEventSource<RootEvent.UserAuthorizationGotInvalid, RootEvent> {
        it
    }