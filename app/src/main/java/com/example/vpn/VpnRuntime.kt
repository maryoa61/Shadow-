package com.example.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionPhase {
    DISCONNECTED,
    STARTING,
    VERIFYING,
    CONNECTED,
    STOPPING,
    ERROR
}

data class VpnRuntimeState(
    val phase: ConnectionPhase = ConnectionPhase.DISCONNECTED,
    val activeCore: CoreEngine? = null,
    val serverName: String? = null,
    val error: String? = null,
    val elapsedSeconds: Long = 0,
    val downloadBytesPerSecond: Long = 0,
    val uploadBytesPerSecond: Long = 0,
    val totalDownloadBytes: Long = 0,
    val totalUploadBytes: Long = 0,
    val pingMs: Int? = null
)

/** In-process state shared by the foreground VPN service and the Compose UI. */
object VpnRuntime {
    private val _state = MutableStateFlow(VpnRuntimeState())
    val state: StateFlow<VpnRuntimeState> = _state.asStateFlow()

    internal fun update(transform: (VpnRuntimeState) -> VpnRuntimeState) {
        _state.value = transform(_state.value)
    }

    internal fun set(state: VpnRuntimeState) {
        _state.value = state
    }
}
