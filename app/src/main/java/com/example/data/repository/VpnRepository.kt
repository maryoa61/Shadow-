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
        // A VPN client must never pretend that generated/example endpoints are
        // connected. Start with an empty server list and wait for a real import.
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
