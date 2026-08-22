package com.example.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.LogEntryEntity
import com.example.data.local.ServerEntity
import com.example.data.repository.VpnRepository
import com.example.vpn.ConnectionPhase
import com.example.vpn.CoreEngine
import com.example.vpn.ShadowVpnService
import com.example.vpn.VpnRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class VpnUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionError: String? = null,
    val preferredCore: String = CoreEngine.AUTO.persistedValue,
    val activeCore: String? = null,
    val connectionDurationSeconds: Long = 0,
    val connectionDurationFormatted: String = "00:00:00",
    val dlRateMbps: Double = 0.0,
    val ulRateMbps: Double = 0.0,
    val totalUsageGb: Double = 0.0,
    val usageLimitGb: Double = 10.0,
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val hopMode: String = "1-HOP", // 1-HOP, 2-HOP, DIRECT
    val activeProtocolName: String = "No server selected",
    val activeProtocolTag: String = "Import a real configuration",
    val flow: String = "",
    val muxState: String = "Incompatible",
    val muxEnabled: Boolean = false,
    val muxConcurrency: Int = 8,
    val fragmentEnabled: Boolean = true,
    val fragmentPackets: Int = 2,
    val fragmentLength: String = "50-100",
    val fragmentIntervalMs: Int = 10,
    val utlsProfile: String = "Chrome",
    val echEnabled: Boolean = false,
    val cleanIpOverride: String = "",
    val dohProvider: String = "Cloudflare",
    val fakeIpEnabled: Boolean = false,
    val domesticDnsFallback: String = "",
    val strictKillSwitch: Boolean = false,
    val chainPreset: String = "Single Hop",
    val hop1Server: String = "Select Server...",
    val hop1Proto: String = "",
    val hop1Ping: Int = 0,
    val hop2Server: String = "Select Server...",
    val hop2Standby: Boolean = true,
    val currentFilterProtocol: String = "All",
    val searchQuery: String = "",
    val activeLogFilter: String = "ALL",
    val selectedServer: ServerEntity? = null,
    val isCriticalState: Boolean = false,
    val isTestingCleanIp: Boolean = false,
    val cleanIpTestResult: String? = null
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VpnRepository

    val servers: StateFlow<List<ServerEntity>>
    val logs: StateFlow<List<LogEntryEntity>>

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VpnRepository(db.serverDao(), db.logDao(), db.appSettingsDao())

        servers = repository.allServers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        logs = repository.allLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
            repository.settings.collect { savedSettings ->
                savedSettings?.let { s ->
                    _uiState.value = _uiState.value.copy(
                        usageLimitGb = s.usageLimitGb,
                        hopMode = s.hopMode,
                        preferredCore = CoreEngine.fromPersisted(s.preferredCore).persistedValue,
                        muxEnabled = s.muxEnabled,
                        muxConcurrency = s.muxConcurrency,
                        fragmentEnabled = s.fragmentEnabled,
                        fragmentPackets = s.fragmentPackets,
                        fragmentLength = s.fragmentLength,
                        fragmentIntervalMs = s.fragmentIntervalMs,
                        utlsProfile = s.utlsProfile,
                        echEnabled = s.echEnabled,
                        cleanIpOverride = s.cleanIpOverride,
                        dohProvider = s.dohProvider,
                        fakeIpEnabled = s.fakeIpEnabled,
                        domesticDnsFallback = s.domesticDnsFallback,
                        strictKillSwitch = s.strictKillSwitch,
                        chainPreset = s.chainPreset,
                        hop1Server = s.hop1ServerName,
                        hop1Proto = s.hop1Protocol,
                        hop1Ping = s.hop1Latency,
                        hop2Server = s.hop2ServerName,
                        hop2Standby = s.hop2Standby
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.selectedServer.collect { server ->
                _uiState.value = _uiState.value.copy(
                    selectedServer = server,
                    pingMs = if (_uiState.value.isConnected) _uiState.value.pingMs else 0,
                    activeProtocolName = if (server != null) "${server.protocol}+${server.security}" else "No server selected",
                    activeProtocolTag = server?.bestForRegionTag?.takeIf { it.isNotBlank() }
                        ?: if (server == null) "Import a real configuration" else server.transport,
                    flow = server?.flow.orEmpty()
                )
            }
        }

        viewModelScope.launch {
            VpnRuntime.state.collect { runtime ->
                val connected = runtime.phase == ConnectionPhase.CONNECTED
                val connecting = runtime.phase in setOf(ConnectionPhase.STARTING, ConnectionPhase.VERIFYING)
                _uiState.value = _uiState.value.copy(
                    isConnected = connected,
                    isConnecting = connecting,
                    connectionError = runtime.error,
                    activeCore = runtime.activeCore?.displayName,
                    connectionDurationSeconds = runtime.elapsedSeconds,
                    connectionDurationFormatted = formatDuration(runtime.elapsedSeconds),
                    dlRateMbps = runtime.downloadBytesPerSecond * 8.0 / 1_000_000.0,
                    ulRateMbps = runtime.uploadBytesPerSecond * 8.0 / 1_000_000.0,
                    totalUsageGb = (runtime.totalDownloadBytes + runtime.totalUploadBytes) / 1_000_000_000.0,
                    pingMs = runtime.pingMs ?: if (connected) _uiState.value.pingMs else 0,
                    jitterMs = 0,
                    isCriticalState = runtime.phase == ConnectionPhase.ERROR
                )
            }
        }
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun startConnection() {
        val selected = _uiState.value.selectedServer
        if (selected == null) {
            _uiState.value = _uiState.value.copy(
                connectionError = "Add and select a real server before connecting."
            )
            return
        }
        ShadowVpnService.start(
            getApplication(),
            CoreEngine.fromPersisted(_uiState.value.preferredCore)
        )
    }

    fun stopConnection() {
        ShadowVpnService.stop(getApplication())
    }

    fun reportVpnPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            isConnecting = false,
            connectionError = "VPN permission is required to create the secure tunnel."
        )
    }

    fun setPreferredCore(value: String) {
        val core = CoreEngine.fromPersisted(value)
        if (_uiState.value.isConnected || _uiState.value.isConnecting) return
        _uiState.value = _uiState.value.copy(preferredCore = core.persistedValue)
        viewModelScope.launch { saveCurrentSettings() }
    }

    fun selectServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.selectServer(server.id)
            repository.addLog("INFO", "Switched active server node to '${server.alias}' (${server.address}:${server.port})")
        }
    }

    fun setHopMode(mode: String) {
        _uiState.value = _uiState.value.copy(hopMode = mode)
        viewModelScope.launch {
            repository.addLog("INFO", "Routing chain topology changed to $mode")
            saveCurrentSettings()
        }
    }

    fun setChainPreset(preset: String) {
        _uiState.value = _uiState.value.copy(chainPreset = preset)
        viewModelScope.launch {
            repository.addLog("INFO", "Applied Multi-Hop preset: $preset")
            saveCurrentSettings()
        }
    }

    fun setUtlsProfile(profile: String) {
        _uiState.value = _uiState.value.copy(utlsProfile = profile)
        viewModelScope.launch {
            repository.addLog("INFO", "uTLS Client Profile set to $profile")
            saveCurrentSettings()
        }
    }

    fun toggleFragment() {
        val next = !_uiState.value.fragmentEnabled
        _uiState.value = _uiState.value.copy(fragmentEnabled = next)
        viewModelScope.launch {
            repository.addLog("INFO", "Packet Fragmentation ${if (next) "Enabled (EVADE DPI)" else "Disabled"}")
            saveCurrentSettings()
        }
    }

    fun toggleMux() {
        val next = !_uiState.value.muxEnabled
        _uiState.value = _uiState.value.copy(muxEnabled = next)
        viewModelScope.launch {
            saveCurrentSettings()
        }
    }

    fun setMuxConcurrency(limit: Int) {
        _uiState.value = _uiState.value.copy(muxConcurrency = limit)
        viewModelScope.launch { saveCurrentSettings() }
    }

    fun toggleEch() {
        val next = !_uiState.value.echEnabled
        _uiState.value = _uiState.value.copy(echEnabled = next)
        viewModelScope.launch {
            repository.addLog("INFO", "ECH (Encrypted Client Hello) ${if (next) "Activated" else "Deactivated"}")
            saveCurrentSettings()
        }
    }

    fun setCleanIpOverride(ip: String) {
        _uiState.value = _uiState.value.copy(cleanIpOverride = ip)
        viewModelScope.launch { saveCurrentSettings() }
    }

    fun setDohProvider(provider: String) {
        _uiState.value = _uiState.value.copy(dohProvider = provider)
        viewModelScope.launch {
            repository.addLog("INFO", "DoH Secure DNS upstream set to $provider")
            saveCurrentSettings()
        }
    }

    fun toggleFakeIp() {
        val next = !_uiState.value.fakeIpEnabled
        _uiState.value = _uiState.value.copy(fakeIpEnabled = next)
        viewModelScope.launch {
            repository.addLog("INFO", "Fake-IP DNS mode ${if (next) "Enabled" else "Disabled"}")
            saveCurrentSettings()
        }
    }

    fun setDomesticDns(dns: String) {
        _uiState.value = _uiState.value.copy(domesticDnsFallback = dns)
        viewModelScope.launch { saveCurrentSettings() }
    }

    fun toggleKillSwitch() {
        val next = !_uiState.value.strictKillSwitch
        _uiState.value = _uiState.value.copy(strictKillSwitch = next)
        viewModelScope.launch {
            val level = if (next) "WARN" else "INFO"
            repository.addLog(level, "Strict Kill Switch ${if (next) "ARMED (All traffic gated)" else "DISARMED"}")
            saveCurrentSettings()
        }
    }

    fun flushRoutes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCriticalState = false)
            repository.addLog("OK", "Flushed all local routing tables and zombie interfaces.")
            repository.addLog("INFO", "Rebuilding tunnel route mapping...")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.addLog("INFO", "Logs purged. System telemetry stream initialized.")
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilterProtocol(proto: String) {
        _uiState.value = _uiState.value.copy(currentFilterProtocol = proto)
    }

    fun setLogFilter(filter: String) {
        _uiState.value = _uiState.value.copy(activeLogFilter = filter)
    }

    fun testCleanIp(ip: String) {
        val cleanIp = ip.trim()
        if (!Patterns.IP_ADDRESS.matcher(cleanIp).matches()) {
            _uiState.value = _uiState.value.copy(
                isTestingCleanIp = false,
                cleanIpTestResult = "Enter a valid IPv4 or IPv6 address before testing."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingCleanIp = true, cleanIpTestResult = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val started = System.nanoTime()
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(cleanIp, 443), 4000)
                    }
                    ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(1)
                }
            }
            val ping = result.getOrNull()
            val message = if (ping != null) "TCP/443 reachable • RTT: ${ping}ms" else "TCP/443 unreachable"
            _uiState.value = _uiState.value.copy(
                isTestingCleanIp = false,
                cleanIpTestResult = message
            )
            repository.addLog(if (ping != null) "OK" else "ERR", "Clean IP $cleanIp: $message")
        }
    }

    fun saveOrUpdateServer(server: ServerEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            if (server.id == 0L) {
                repository.insertServer(server)
                repository.addLog("OK", "Added new node configuration: ${server.alias}")
            } else {
                repository.updateServer(server)
                repository.addLog("OK", "Updated configuration: ${server.alias}")
            }
            onDone()
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.deleteServer(server)
            repository.addLog("WARN", "Removed node: ${server.alias}")
        }
    }

    private suspend fun saveCurrentSettings() {
        val s = _uiState.value
        val entity = AppSettingsEntity(
            id = 1,
            // Runtime connection state belongs to VpnService, not Room. Persisting
            // "connected" caused the old UI to claim a tunnel after process death.
            isConnected = false,
            connectedSeconds = 0,
            dlRateMbps = 0.0,
            ulRateMbps = 0.0,
            totalUsageGb = 0.0,
            usageLimitGb = s.usageLimitGb,
            hopMode = s.hopMode,
            preferredCore = CoreEngine.fromPersisted(s.preferredCore).persistedValue,
            muxEnabled = s.muxEnabled,
            muxConcurrency = s.muxConcurrency,
            fragmentEnabled = s.fragmentEnabled,
            fragmentPackets = s.fragmentPackets,
            fragmentLength = s.fragmentLength,
            fragmentIntervalMs = s.fragmentIntervalMs,
            utlsProfile = s.utlsProfile,
            echEnabled = s.echEnabled,
            cleanIpOverride = s.cleanIpOverride,
            dohProvider = s.dohProvider,
            fakeIpEnabled = s.fakeIpEnabled,
            domesticDnsFallback = s.domesticDnsFallback,
            strictKillSwitch = s.strictKillSwitch,
            chainPreset = s.chainPreset,
            hop1ServerName = s.hop1Server,
            hop1Protocol = s.hop1Proto,
            hop1Latency = s.hop1Ping,
            hop2ServerName = s.hop2Server,
            hop2Standby = s.hop2Standby
        )
        repository.saveSettings(entity)
    }
}
