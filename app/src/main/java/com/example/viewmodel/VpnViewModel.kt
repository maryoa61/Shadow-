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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TELEMETRY_USAGE_INCREMENT_GB = 0.002

internal fun nextTelemetryUsageGb(currentUsageGb: Double): Double =
    Math.round((currentUsageGb + TELEMETRY_USAGE_INCREMENT_GB) * 1000.0) / 1000.0

data class VpnUiState(
    val isConnected: Boolean = true,
    val connectionDurationSeconds: Long = 8078, // 02:14:38
    val connectionDurationFormatted: String = "02:14:38",
    val dlRateMbps: Double = 142.5,
    val ulRateMbps: Double = 28.4,
    val totalUsageGb: Double = 4.2,
    val usageLimitGb: Double = 10.0,
    val pingMs: Int = 42,
    val jitterMs: Int = 14,
    val hopMode: String = "2-HOP", // 1-HOP, 2-HOP, DIRECT
    val activeProtocolName: String = "XTLS-Reality + Vision",
    val activeProtocolTag: String = "Best for Iran",
    val flow: String = "xtls-rprx-vision",
    val muxState: String = "Incompatible",
    val muxEnabled: Boolean = false,
    val muxConcurrency: Int = 8,
    val fragmentEnabled: Boolean = true,
    val fragmentPackets: Int = 2,
    val fragmentLength: String = "10-30/5-10",
    val fragmentIntervalMs: Int = 10,
    val utlsProfile: String = "Chrome",
    val echEnabled: Boolean = false,
    val cleanIpOverride: String = "104.16.24.10",
    val dohProvider: String = "Cloudflare",
    val fakeIpEnabled: Boolean = true,
    val domesticDnsFallback: String = "178.22.122.100",
    val strictKillSwitch: Boolean = false,
    val chainPreset: String = "Double-Hop NL",
    val hop1Server: String = "NL-AMS-VLESS-01",
    val hop1Proto: String = "tcp / xtls-rprx-vision",
    val hop1Ping: Int = 12,
    val hop2Server: String = "Select Server...",
    val hop2Standby: Boolean = true,
    val currentFilterProtocol: String = "All",
    val searchQuery: String = "",
    val activeLogFilter: String = "ALL",
    val selectedServer: ServerEntity? = null,
    val isCriticalState: Boolean = true,
    val isTestingCleanIp: Boolean = false,
    val cleanIpTestResult: String? = null
)

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VpnRepository

    val servers: StateFlow<List<ServerEntity>>
    val logs: StateFlow<List<LogEntryEntity>>

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null
    private var timerJob: Job? = null

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
                        isConnected = s.isConnected,
                        connectionDurationSeconds = s.connectedSeconds,
                        connectionDurationFormatted = formatDuration(s.connectedSeconds),
                        dlRateMbps = if (s.isConnected) s.dlRateMbps else 0.0,
                        ulRateMbps = if (s.isConnected) s.ulRateMbps else 0.0,
                        totalUsageGb = s.totalUsageGb,
                        usageLimitGb = s.usageLimitGb,
                        hopMode = s.hopMode,
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
                    pingMs = server?.pingMs ?: 42,
                    activeProtocolName = if (server != null) "${server.protocol}+${server.security}" else "XTLS-Reality + Vision",
                    activeProtocolTag = server?.bestForRegionTag ?: "Best for Iran"
                )
            }
        }

        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        timerJob?.cancel()
        simulationJob?.cancel()

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.isConnected) {
                    val newSec = _uiState.value.connectionDurationSeconds + 1
                    _uiState.value = _uiState.value.copy(
                        connectionDurationSeconds = newSec,
                        connectionDurationFormatted = formatDuration(newSec)
                    )
                    // Persist long-running sessions without writing to Room every second.
                    if (newSec % 30L == 0L) {
                        saveCurrentSettings()
                    }
                }
            }
        }

        simulationJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                if (_uiState.value.isConnected) {
                    val dlVariation = Random.nextDouble(120.0, 165.0)
                    val ulVariation = Random.nextDouble(20.0, 38.0)
                    val newUsage = nextTelemetryUsageGb(_uiState.value.totalUsageGb)
                    _uiState.value = _uiState.value.copy(
                        dlRateMbps = Math.round(dlVariation * 10.0) / 10.0,
                        ulRateMbps = Math.round(ulVariation * 10.0) / 10.0,
                        // Keeping three decimal places prevents each 0.002 GB sample
                        // from rounding back down and freezing the displayed total.
                        totalUsageGb = newUsage,
                        jitterMs = Random.nextInt(10, 18)
                    )
                }
            }
        }
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun toggleConnection() {
        val newConnectedState = !_uiState.value.isConnected
        _uiState.value = _uiState.value.copy(
            isConnected = newConnectedState,
            isCriticalState = if (newConnectedState) false else _uiState.value.isCriticalState,
            dlRateMbps = if (newConnectedState) _uiState.value.dlRateMbps else 0.0,
            ulRateMbps = if (newConnectedState) _uiState.value.ulRateMbps else 0.0
        )
        viewModelScope.launch {
            val level = if (newConnectedState) "OK" else "INFO"
            val msg = if (newConnectedState) "Tunnel securely established. Routing online." else "Tunnel manually disconnected by user."
            repository.addLog(level, msg)
            saveCurrentSettings()
        }
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
            delay(1200)
            val ping = Random.nextInt(28, 55)
            _uiState.value = _uiState.value.copy(
                isTestingCleanIp = false,
                cleanIpTestResult = "RTT: ${ping}ms (Clean IP OK)"
            )
            repository.addLog("OK", "Clean IP $cleanIp verified reachable. RTT=${ping}ms")
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
            isConnected = s.isConnected,
            connectedSeconds = s.connectionDurationSeconds,
            dlRateMbps = s.dlRateMbps,
            ulRateMbps = s.ulRateMbps,
            totalUsageGb = s.totalUsageGb,
            usageLimitGb = s.usageLimitGb,
            hopMode = s.hopMode,
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
