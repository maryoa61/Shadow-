package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alias: String,
    val address: String,
    val port: Int,
    val uuid: String,
    val protocol: String = "VLESS", // VLESS, VMess, Trojan, Shadowsocks, Hysteria2
    val security: String = "Reality", // None, TLS, Reality
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "/",
    val sni: String = "microsoft.com",
    val flow: String = "xtls-rprx-vision", // xtls-rprx-vision, none
    val transport: String = "TCP", // TCP, WS, gRPC, XHTTP, H2, mKCP
    val cleanIp: String = "",
    val pingMs: Int = 42,
    val throughput: String = "1.2 Gbps",
    val packetLoss: String = "0.0%",
    val handshake: String = "TLS 1.3",
    val countryCode: String = "DE",
    val countryName: String = "Germany",
    val ipAddress: String = "185.12.44.10",
    val capacity: String = "120Mbps",
    val bestForRegionTag: String = "Best for Iran",
    val isSelected: Boolean = false
)
