package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isConnected: Boolean = false,
    val connectedSeconds: Long = 0,
    val dlRateMbps: Double = 0.0,
    val ulRateMbps: Double = 0.0,
    val totalUsageGb: Double = 0.0,
    val usageLimitGb: Double = 10.0,
    val hopMode: String = "1-HOP", // 1-HOP, 2-HOP, DIRECT
    val preferredCore: String = "AUTO", // AUTO, XRAY, SING_BOX
    // Anti-DPI & Chain Settings
    val muxEnabled: Boolean = false,
    val muxConcurrency: Int = 8,
    val fragmentEnabled: Boolean = true,
    val fragmentPackets: Int = 2,
    val fragmentLength: String = "50-100",
    val fragmentIntervalMs: Int = 10,
    val utlsProfile: String = "Chrome", // Chrome, Firefox, Safari, iOS, Randomized
    val echEnabled: Boolean = false,
    val cleanIpOverride: String = "",
    val dohProvider: String = "Cloudflare", // Cloudflare, Google
    val fakeIpEnabled: Boolean = false,
    val domesticDnsFallback: String = "",
    val strictKillSwitch: Boolean = false,
    // Chain configuration presets
    val chainPreset: String = "Single Hop",
    val hop1ServerName: String = "Select Server...",
    val hop1Protocol: String = "",
    val hop1Latency: Int = 0,
    val hop2ServerName: String = "Select Server...",
    val hop2Standby: Boolean = true
)
