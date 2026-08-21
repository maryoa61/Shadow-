package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isConnected: Boolean = true,
    val connectedSeconds: Long = 8078, // 02:14:38
    val dlRateMbps: Double = 142.5,
    val ulRateMbps: Double = 28.4,
    val totalUsageGb: Double = 4.2,
    val usageLimitGb: Double = 10.0,
    val hopMode: String = "2-HOP", // 1-HOP, 2-HOP, DIRECT
    // Anti-DPI & Chain Settings
    val muxEnabled: Boolean = false,
    val muxConcurrency: Int = 8,
    val fragmentEnabled: Boolean = true,
    val fragmentPackets: Int = 2,
    val fragmentLength: String = "10-100",
    val fragmentIntervalMs: Int = 10,
    val utlsProfile: String = "Chrome", // Chrome, Firefox, Safari, iOS, Randomized
    val echEnabled: Boolean = false,
    val cleanIpOverride: String = "104.16.24.10",
    val dohProvider: String = "Cloudflare", // Cloudflare, Google
    val fakeIpEnabled: Boolean = true,
    val domesticDnsFallback: String = "178.22.122.100",
    val strictKillSwitch: Boolean = false,
    // Chain configuration presets
    val chainPreset: String = "Double-Hop NL",
    val hop1ServerName: String = "NL-AMS-VLESS-01",
    val hop1Protocol: String = "tcp / xtls-rprx-vision",
    val hop1Latency: Int = 12,
    val hop2ServerName: String = "Select Server...",
    val hop2Standby: Boolean = true
)
