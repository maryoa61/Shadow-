package com.example.vpn

import com.example.data.local.ServerEntity
import org.json.JSONArray
import org.json.JSONObject

internal object VpnConfigBuilder {
    private const val SOCKS_PORT = 10808

    fun validate(server: ServerEntity, engine: CoreEngine): String? {
        if (server.address.isBlank()) return "Server address is empty."
        if (server.address.contains("shadownet.core", ignoreCase = true)) {
            return "This is a removed demo server. Add a real server configuration."
        }
        if (server.port !in 1..65535) return "Server port must be between 1 and 65535."

        val protocol = normalizeProtocol(server.protocol)
        if (protocol !in setOf("vless", "vmess", "trojan", "shadowsocks", "hysteria2")) {
            return "Protocol '${server.protocol}' is not supported by the VPN runtime."
        }
        if (server.uuid.isBlank()) {
            return if (protocol in setOf("vless", "vmess")) "UUID is empty." else "Password is empty."
        }
        if (protocol == "shadowsocks" && server.method.isBlank()) {
            return "Shadowsocks cipher/method is empty."
        }
        if (server.security.equals("Reality", ignoreCase = true)) {
            if (server.publicKey.isBlank()) return "Reality public key is empty."
            if (server.sni.isBlank()) return "Reality server name (SNI) is empty."
        }

        val transport = normalizeTransport(server.transport)
        if (engine == CoreEngine.SING_BOX && transport in setOf("xhttp", "kcp")) {
            return "${server.transport} is supported through the Xray core. Select Xray or Auto."
        }
        return null
    }

    fun buildXray(
        server: ServerEntity,
        utlsProfile: String,
        mtu: Int = 1500,
        fragmentEnabled: Boolean = false,
        fragmentLength: String = "50-100",
        fragmentIntervalMs: Int = 10
    ): String {
        validate(server, CoreEngine.XRAY)?.let { error(it) }
        return xrayBaseConfig(
            proxyOutbound = buildXrayProxyOutbound(
                server,
                utlsProfile,
                fragmentEnabled,
                fragmentLength,
                fragmentIntervalMs
            ),
            mtu = mtu
        ).toString()
    }

    fun buildXrayToSingBoxBridge(mtu: Int = 1500): String {
        val proxy = JSONObject()
            .put("tag", "proxy")
            .put("protocol", "socks")
            .put(
                "settings",
                JSONObject()
                    .put("address", "127.0.0.1")
                    .put("port", SOCKS_PORT)
            )
        return xrayBaseConfig(proxy, mtu).toString()
    }

    fun buildSingBox(server: ServerEntity, utlsProfile: String): String {
        validate(server, CoreEngine.SING_BOX)?.let { error(it) }

        val config = JSONObject()
            .put("log", JSONObject().put("level", "warn").put("timestamp", true))
            .put("dns", buildSingBoxDns())
            .put(
                "inbounds",
                JSONArray().put(
                    JSONObject()
                        .put("type", "mixed")
                        .put("tag", "mixed-in")
                        .put("listen", "127.0.0.1")
                        .put("listen_port", SOCKS_PORT)
                )
            )
            .put(
                "outbounds",
                JSONArray()
                    .put(buildSingBoxProxyOutbound(server, utlsProfile))
                    .put(JSONObject().put("type", "direct").put("tag", "direct"))
            )
            .put(
                "route",
                JSONObject()
                    .put(
                        "rules",
                        JSONArray()
                            .put(JSONObject().put("action", "sniff"))
                            .put(JSONObject().put("protocol", "dns").put("action", "hijack-dns"))
                            .put(JSONObject().put("ip_is_private", true).put("outbound", "direct"))
                    )
                    .put(
                        "default_domain_resolver",
                        JSONObject().put("server", "dns-local").put("strategy", "prefer_ipv4")
                    )
                    .put("final", "proxy")
            )
        return config.toString()
    }

    private fun xrayBaseConfig(proxyOutbound: JSONObject, mtu: Int): JSONObject {
        val tunInbound = JSONObject()
            .put("tag", "tun")
            .put("protocol", "tun")
            .put(
                "settings",
                JSONObject()
                    .put("name", "xray0")
                    .put("MTU", mtu)
                    .put("userLevel", 8)
            )
            .put(
                "sniffing",
                JSONObject()
                    .put("enabled", true)
                    .put("destOverride", JSONArray(listOf("http", "tls", "quic")))
            )

        val direct = JSONObject()
            .put("tag", "direct")
            .put("protocol", "freedom")
            .put("settings", JSONObject().put("domainStrategy", "UseIP"))

        val privateNetworks = JSONArray(
            listOf(
                "127.0.0.0/8",
                "10.0.0.0/8",
                "172.16.0.0/12",
                "192.168.0.0/16",
                "::1/128",
                "fc00::/7",
                "fe80::/10"
            )
        )

        return JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("stats", JSONObject())
            .put(
                "policy",
                JSONObject()
                    .put(
                        "levels",
                        JSONObject().put(
                            "8",
                            JSONObject()
                                .put("handshake", 8)
                                .put("connIdle", 300)
                                .put("uplinkOnly", 1)
                                .put("downlinkOnly", 1)
                        )
                    )
                    .put(
                        "system",
                        JSONObject()
                            .put("statsOutboundUplink", true)
                            .put("statsOutboundDownlink", true)
                    )
            )
            .put("inbounds", JSONArray().put(tunInbound))
            .put("outbounds", JSONArray().put(proxyOutbound).put(direct))
            .put(
                "routing",
                JSONObject()
                    .put("domainStrategy", "AsIs")
                    .put(
                        "rules",
                        JSONArray().put(
                            JSONObject()
                                .put("type", "field")
                                .put("ip", privateNetworks)
                                .put("outboundTag", "direct")
                        )
                    )
            )
    }

    private fun buildXrayProxyOutbound(
        server: ServerEntity,
        utlsProfile: String,
        fragmentEnabled: Boolean,
        fragmentLength: String,
        fragmentIntervalMs: Int
    ): JSONObject {
        val protocol = normalizeProtocol(server.protocol)
        val settings = JSONObject()
            .put("address", server.address.trim())
            .put("port", server.port)
            .put("level", 8)

        when (protocol) {
            "vless" -> settings
                .put("id", server.uuid.trim())
                .put("encryption", "none")
                .putIfNotBlank("flow", server.flow)
            "vmess" -> settings
                .put("id", server.uuid.trim())
                .put("security", "auto")
            "trojan" -> settings
                .put("password", server.uuid)
                .putIfNotBlank("flow", server.flow)
            "shadowsocks" -> settings
                .put("password", server.uuid)
                .put("method", server.method)
            "hysteria2" -> settings.put("version", 2)
        }

        val stream = buildXrayStreamSettings(
            server,
            utlsProfile,
            protocol,
            fragmentEnabled,
            fragmentLength,
            fragmentIntervalMs
        )
        if (protocol == "hysteria2") {
            stream.put("network", "hysteria")
            stream.put(
                "hysteriaSettings",
                JSONObject().put("version", 2).put("auth", server.uuid)
            )
        }

        return JSONObject()
            .put("tag", "proxy")
            .put("protocol", if (protocol == "hysteria2") "hysteria" else protocol)
            .put("settings", settings)
            .put("streamSettings", stream)
            .put("mux", JSONObject().put("enabled", false))
    }

    private fun buildXrayStreamSettings(
        server: ServerEntity,
        utlsProfile: String,
        protocol: String,
        fragmentEnabled: Boolean,
        fragmentLength: String,
        fragmentIntervalMs: Int
    ): JSONObject {
        val transport = normalizeTransport(server.transport)
        val stream = JSONObject().put("network", transport)
        val path = normalizedPath(server.spiderX)

        when (transport) {
            "tcp" -> stream.put(
                "tcpSettings",
                JSONObject().put("header", JSONObject().put("type", "none"))
            )
            "ws" -> stream.put(
                "wsSettings",
                JSONObject()
                    .put("path", path)
                    .put("headers", JSONObject().putIfNotBlank("Host", server.sni))
            )
            "grpc" -> stream.put(
                "grpcSettings",
                JSONObject()
                    .put("serviceName", path.trim('/'))
                    .put("multiMode", false)
            )
            "xhttp" -> stream.put(
                "xhttpSettings",
                JSONObject()
                    .put("path", path)
                    .putIfNotBlank("host", server.sni)
                    .put("mode", "auto")
            )
            "h2" -> stream.put(
                "httpSettings",
                JSONObject()
                    .put("path", path)
                    .put("host", JSONArray().apply { if (server.sni.isNotBlank()) put(server.sni) })
            )
            "kcp" -> stream.put(
                "kcpSettings",
                JSONObject().put("header", JSONObject().put("type", "none"))
            )
        }

        val security = when {
            protocol == "hysteria2" -> "tls"
            server.security.equals("none", ignoreCase = true) -> "none"
            else -> server.security.lowercase()
        }
        stream.put("security", security)

        if (security == "reality") {
            stream.put(
                "realitySettings",
                JSONObject()
                    .put("serverName", server.sni)
                    .put("fingerprint", normalizeFingerprint(utlsProfile))
                    .put("publicKey", server.publicKey)
                    .put("shortId", server.shortId)
                    .put("spiderX", path)
            )
        } else if (security == "tls") {
            stream.put(
                "tlsSettings",
                JSONObject()
                    .putIfNotBlank("serverName", server.sni)
                    .put("fingerprint", normalizeFingerprint(utlsProfile))
                    .put("allowInsecure", false)
            )
        }

        if (fragmentEnabled && security in setOf("tls", "reality")) {
            val packets = if (security == "reality") "1-3" else "tlshello"
            stream.put(
                "finalmask",
                JSONObject().put(
                    "tcp",
                    JSONArray().put(
                        JSONObject()
                            .put("type", "fragment")
                            .put(
                                "settings",
                                JSONObject()
                                    .put("packets", packets)
                                    .put("length", fragmentLength.ifBlank { "50-100" })
                                    .put("delay", fragmentIntervalMs.coerceAtLeast(0).toString())
                                    .put("maxSplit", "10")
                            )
                    )
                )
            )
        }
        return stream
    }

    private fun buildSingBoxProxyOutbound(server: ServerEntity, utlsProfile: String): JSONObject {
        val protocol = normalizeProtocol(server.protocol)
        val outbound = JSONObject()
            .put("type", protocol)
            .put("tag", "proxy")
            .put("server", server.address.trim())
            .put("server_port", server.port)

        when (protocol) {
            "vless" -> outbound
                .put("uuid", server.uuid.trim())
                .putIfNotBlank("flow", server.flow)
                .put("packet_encoding", "xudp")
            "vmess" -> outbound
                .put("uuid", server.uuid.trim())
                .put("security", "auto")
                .put("alter_id", 0)
            "trojan" -> outbound.put("password", server.uuid)
            "shadowsocks" -> outbound
                .put("password", server.uuid)
                .put("method", server.method)
            "hysteria2" -> outbound.put("password", server.uuid)
        }

        buildSingBoxTls(server, utlsProfile, protocol)?.let { outbound.put("tls", it) }
        buildSingBoxTransport(server)?.let { outbound.put("transport", it) }
        return outbound
    }

    private fun buildSingBoxTls(
        server: ServerEntity,
        utlsProfile: String,
        protocol: String
    ): JSONObject? {
        val enabled = protocol == "hysteria2" ||
            server.security.equals("tls", ignoreCase = true) ||
            server.security.equals("reality", ignoreCase = true)
        if (!enabled) return null

        val tls = JSONObject()
            .put("enabled", true)
            .putIfNotBlank("server_name", server.sni)
            .put(
                "utls",
                JSONObject()
                    .put("enabled", true)
                    .put("fingerprint", normalizeFingerprint(utlsProfile))
            )
        if (server.security.equals("reality", ignoreCase = true)) {
            tls.put(
                "reality",
                JSONObject()
                    .put("enabled", true)
                    .put("public_key", server.publicKey)
                    .put("short_id", server.shortId)
            )
        }
        return tls
    }

    private fun buildSingBoxTransport(server: ServerEntity): JSONObject? {
        val path = normalizedPath(server.spiderX)
        return when (normalizeTransport(server.transport)) {
            "tcp" -> null
            "ws" -> JSONObject()
                .put("type", "ws")
                .put("path", path)
                .put("headers", JSONObject().putIfNotBlank("Host", server.sni))
            "grpc" -> JSONObject()
                .put("type", "grpc")
                .put("service_name", path.trim('/'))
            "h2" -> JSONObject()
                .put("type", "http")
                .put("path", path)
                .put("host", JSONArray().apply { if (server.sni.isNotBlank()) put(server.sni) })
            else -> null
        }
    }

    private fun buildSingBoxDns(): JSONObject = JSONObject()
        .put(
            "servers",
            JSONArray()
                .put(
                    JSONObject()
                        .put("type", "https")
                        .put("tag", "dns-remote")
                        .put("server", "1.1.1.1")
                        .put("server_port", 443)
                        .put("path", "/dns-query")
                        .put("detour", "proxy")
                )
                .put(JSONObject().put("type", "local").put("tag", "dns-local"))
        )
        .put("strategy", "prefer_ipv4")
        .put("final", "dns-remote")
        .put("independent_cache", true)

    private fun normalizeProtocol(value: String): String = when (value.trim().lowercase()) {
        "ss" -> "shadowsocks"
        "hy2", "hysteria" -> "hysteria2"
        else -> value.trim().lowercase()
    }

    private fun normalizeTransport(value: String): String = when (value.trim().lowercase()) {
        "websocket" -> "ws"
        "gprc" -> "grpc"
        "splithttp" -> "xhttp"
        "http", "http2" -> "h2"
        "mkcp" -> "kcp"
        else -> value.trim().lowercase()
    }

    private fun normalizeFingerprint(value: String): String = when (value.trim().lowercase()) {
        "ios" -> "ios"
        "randomized", "random" -> "randomized"
        "" -> "chrome"
        else -> value.trim().lowercase()
    }

    private fun normalizedPath(value: String): String {
        val clean = value.trim().ifEmpty { "/" }
        return if (clean.startsWith('/')) clean else "/$clean"
    }

    private fun JSONObject.putIfNotBlank(key: String, value: String): JSONObject {
        if (value.isNotBlank()) put(key, value.trim())
        return this
    }
}
