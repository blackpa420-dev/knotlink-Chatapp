package com.example.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-local bridge between the dedicated CallActivity and MainActivity. */
data class ActiveCallBridgeState(
    val callId: String = "",
    val peerId: String = "",
    val peerName: String = "",
    val callType: String = "AUDIO",
    val startedAt: Long = System.currentTimeMillis(),
    val isConnected: Boolean = false
)

object ActiveCallBridge {
    private val _state = MutableStateFlow<ActiveCallBridgeState?>(null)
    val state: StateFlow<ActiveCallBridgeState?> = _state

    fun start(callId: String, peerId: String, peerName: String, callType: String, isConnected: Boolean = false) {
        _state.value = ActiveCallBridgeState(
            callId = callId,
            peerId = peerId,
            peerName = peerName,
            callType = callType.uppercase(),
            startedAt = System.currentTimeMillis(),
            isConnected = isConnected
        )
    }

    fun setConnected(connectedAt: Long? = null) {
        val current = _state.value ?: return
        _state.value = current.copy(
            startedAt = connectedAt ?: current.startedAt,
            isConnected = true
        )
    }

    fun clear(callId: String? = null) {
        val current = _state.value
        if (callId.isNullOrBlank() || current?.callId.isNullOrBlank() || current?.callId == callId) {
            _state.value = null
        }
    }
}
