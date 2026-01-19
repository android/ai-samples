package com.thomasezan.gemini_live_xr

sealed interface LiveSessionState {
    data object NotReady : LiveSessionState
    data object Ready : LiveSessionState
    data object Running : LiveSessionState
    data object Error : LiveSessionState
}
