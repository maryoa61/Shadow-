package com.example.vpn

/** VPN engines bundled by the app. AUTO tries the best engine first and falls back to the other. */
enum class CoreEngine(val persistedValue: String, val displayName: String) {
    AUTO("AUTO", "AUTO"),
    XRAY("XRAY", "XRAY"),
    SING_BOX("SING_BOX", "SING-BOX");

    companion object {
        fun fromPersisted(value: String?): CoreEngine =
            entries.firstOrNull { it.persistedValue.equals(value, ignoreCase = true) } ?: AUTO
    }
}

internal object CoreResolver {
    fun candidates(
        preferred: CoreEngine,
        protocol: String,
        transport: String
    ): List<CoreEngine> {
        if (preferred != CoreEngine.AUTO) return listOf(preferred)

        val normalizedProtocol = protocol.trim().lowercase()
        val normalizedTransport = transport.trim().lowercase()

        // XHTTP and mKCP are handled by Xray. Hysteria2 and Shadowsocks are
        // generally more complete in sing-box. Common protocols get a real
        // fallback rather than only changing a label in the UI.
        return when {
            normalizedTransport in setOf("xhttp", "splithttp", "mkcp", "kcp") ->
                listOf(CoreEngine.XRAY)
            normalizedProtocol in setOf("hysteria", "hysteria2", "hy2", "shadowsocks", "ss") ->
                listOf(CoreEngine.SING_BOX, CoreEngine.XRAY)
            else -> listOf(CoreEngine.XRAY, CoreEngine.SING_BOX)
        }
    }
}
