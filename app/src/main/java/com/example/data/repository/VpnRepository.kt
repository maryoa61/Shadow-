package com.example.data.repository

import com.example.data.local.AppSettingsDao
import com.example.data.local.AppSettingsEntity
import com.example.data.local.LogDao
import com.example.data.local.LogEntryEntity
import com.example.data.local.ServerDao
import com.example.data.local.ServerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VpnRepository(
    private val serverDao: ServerDao,
    private val logDao: LogDao,
    private val appSettingsDao: AppSettingsDao
) {
    val allServers: Flow<List<ServerEntity>> = serverDao.getAllServers()
    val selectedServer: Flow<ServerEntity?> = serverDao.getSelectedServer()
    val allLogs: Flow<List<LogEntryEntity>> = logDao.getAllLogs()
    val settings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()

    suspend fun initializeDefaultDataIfEmpty() {
        if (serverDao.getServerCount() == 0) {
            val defaultServers = listOf(
                ServerEntity(
                    alias = "Frankfurt Edge 1",
                    address = "fra-01.shadownet.core",
                    port = 443,
                    uuid = "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                    protocol = "VLESS",
                    security = "Reality",
                    publicKey = "abcd1234efgh5678ijkl9012mnop",
                    shortId = "16",
                    spiderX = "/",
                    sni = "microsoft.com",
                    flow = "xtls-rprx-vision",
                    transport = "TCP",
                    cleanIp = "104.16.24.10",
                    pingMs = 42,
                    throughput = "1.2 Gbps",
                    packetLoss = "0.0%",
                    handshake = "TLS 1.3",
                    countryCode = "DE",
                    countryName = "Germany",
                    ipAddress = "185.12.44.10",
                    capacity = "120Mbps",
                    bestForRegionTag = "Best for Iran",
                    isSelected = true
                ),
                ServerEntity(
                    alias = "Istanbul Core",
                    address = "ist-02.shadownet.core",
                    port = 443,
                    uuid = "e7f8a9b0-1234-5678-90ab-cdef12345678",
                    protocol = "VLESS",
                    security = "Reality",
                    publicKey = "tr9876543210zyxwvutsrqponmlkj",
                    shortId = "24",
                    spiderX = "/stream",
                    sni = "cloudflare.com",
                    flow = "xtls-rprx-vision",
                    transport = "WS",
                    cleanIp = "172.67.180.20",
                    pingMs = 88,
                    throughput = "850 Mbps",
                    packetLoss = "1.2%",
                    handshake = "XTLS-Vision",
                    countryCode = "TR",
                    countryName = "Turkey",
                    ipAddress = "176.240.10.55",
                    capacity = "95Mbps",
                    bestForRegionTag = "Low Latency",
                    isSelected = false
                ),
                ServerEntity(
                    alias = "Dubai Relay",
                    address = "dxb-03.shadownet.core",
                    port = 8443,
                    uuid = "99887766-5544-3322-1100-aabbccddeeff",
                    protocol = "Trojan",
                    security = "TLS",
                    publicKey = "",
                    shortId = "",
                    spiderX = "",
                    sni = "apple.com",
                    flow = "none",
                    transport = "gRPC",
                    cleanIp = "104.18.20.15",
                    pingMs = 145,
                    throughput = "400 Mbps",
                    packetLoss = "5.4%",
                    handshake = "TLS 1.2",
                    countryCode = "AE",
                    countryName = "UAE",
                    ipAddress = "194.143.12.8",
                    capacity = "60Mbps",
                    bestForRegionTag = "Anti-Filter",
                    isSelected = false
                ),
                ServerEntity(
                    alias = "Singapore Matrix",
                    address = "sg-01.shadownet.core",
                    port = 443,
                    uuid = "12345678-abcd-ef01-2345-6789abcdef01",
                    protocol = "VMess",
                    security = "TLS",
                    publicKey = "",
                    shortId = "",
                    spiderX = "",
                    sni = "zoom.us",
                    flow = "none",
                    transport = "XHTTP",
                    cleanIp = "104.21.35.40",
                    pingMs = 190,
                    throughput = "600 Mbps",
                    packetLoss = "2.1%",
                    handshake = "TLS 1.3",
                    countryCode = "SG",
                    countryName = "Singapore",
                    ipAddress = "139.180.200.4",
                    capacity = "150Mbps",
                    bestForRegionTag = "Gaming Route",
                    isSelected = false
                )
            )
            serverDao.insertServers(defaultServers)

            val initialLogs = listOf(
                LogEntryEntity(timeFormatted = "14:02:11", level = "INFO", message = "Initializing core routing module..."),
                LogEntryEntity(timeFormatted = "14:02:11", level = "INFO", message = "Loading configuration 'us-east-shadow-1'"),
                LogEntryEntity(timeFormatted = "14:02:11", level = "INFO", message = "Binding to local interface 127.0.0.1:10808"),
                LogEntryEntity(timeFormatted = "14:02:12", level = "OK", message = "Local SOCKS5 proxy started."),
                LogEntryEntity(timeFormatted = "14:02:12", level = "DBG", message = "Resolving target domain -> 198.51.100.44"),
                LogEntryEntity(timeFormatted = "14:02:12", level = "INFO", message = "Initiating VLESS protocol handshake..."),
                LogEntryEntity(timeFormatted = "14:02:12", level = "INFO", message = "TLS connection established (uTLS fingerprint applied)"),
                LogEntryEntity(timeFormatted = "14:02:13", level = "OK", message = "Tunnel securely established. Traffic flowing."),
                LogEntryEntity(timeFormatted = "14:02:15", level = "DBG", message = "rx: 14.2 KB / tx: 3.1 KB"),
                LogEntryEntity(timeFormatted = "14:02:16", level = "ERR", message = "Read timeout on upstream socket."),
                LogEntryEntity(timeFormatted = "14:02:16", level = "CRIT", message = "ZOMBIE TUNNEL DETECTED. Connection dropped by peer."),
                LogEntryEntity(timeFormatted = "14:02:16", level = "INFO", message = "Attempting automatic reconnection (1/3)..."),
                LogEntryEntity(timeFormatted = "14:02:17", level = "DBG", message = "Tearing down local listeners...")
            )
            logDao.insertLogs(initialLogs)
        }

        // Settings and servers have independent lifecycles. A restored database can
        // contain servers without the singleton settings row, so seed it separately.
        if (appSettingsDao.getSettings().firstOrNull() == null) {
            appSettingsDao.saveSettings(AppSettingsEntity())
        }
    }

    suspend fun insertServer(server: ServerEntity): Long {
        val shouldSelect = server.isSelected || serverDao.getServerCount() == 0
        val insertedId = serverDao.insertServer(server.copy(isSelected = false))
        if (shouldSelect) {
            serverDao.setSelectedServer(insertedId)
        }
        return insertedId
    }

    suspend fun updateServer(server: ServerEntity) = serverDao.updateServer(server)

    suspend fun deleteServer(server: ServerEntity) {
        val wasSelected = serverDao.getSelectedServerNow()?.id == server.id
        serverDao.deleteServer(server)

        // Keep the invariant that a non-empty server list always has one active node.
        if (wasSelected) {
            serverDao.getFirstServer()?.let { fallback ->
                serverDao.setSelectedServer(fallback.id)
            }
        }
    }
    suspend fun selectServer(serverId: Long) = serverDao.setSelectedServer(serverId)
    suspend fun getServerById(id: Long) = serverDao.getServerById(id)

    suspend fun saveSettings(settings: AppSettingsEntity) = appSettingsDao.saveSettings(settings)
    suspend fun getCurrentSettings(): AppSettingsEntity = appSettingsDao.getSettings().firstOrNull() ?: AppSettingsEntity()

    suspend fun addLog(level: String, message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = sdf.format(Date())
        logDao.insertLog(LogEntryEntity(timeFormatted = timeStr, level = level, message = message))
    }

    suspend fun clearLogs() = logDao.clearAllLogs()
}
