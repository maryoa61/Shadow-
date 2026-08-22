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
    val method: String = "",
    val protocol: String = "VLESS", // VLESS, VMess, Trojan, Shadowsocks, Hysteria2
    val security: String = "Reality", // None, TLS, Reality
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "/",
    val sni: String = "microsoft.com",
    val flow: String = "xtls-rprx-vision", // xtls-rprx-vision, none
    val transport: String = "TCP", // TCP, WS, gRPC, XHTTP, H2, mKCP
    val cleanIp: String = "",
    val pingMs: Int = 0,
    val throughput: String = "",
    val packetLoss: String = "",
    val handshake: String = "",
    val countryCode: String = "",
    val countryName: String = "",
    val ipAddress: String = "",
    val capacity: String = "",
    val bestForRegionTag: String = "",
    val isSelected: Boolean = false
)
