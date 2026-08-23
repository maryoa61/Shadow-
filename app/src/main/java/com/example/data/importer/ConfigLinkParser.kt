package com.example.data.importer

import android.net.Uri
import android.util.Base64
import com.example.data.local.ServerEntity
import org.json.JSONObject

/**
 * Outcome of parsing a single configuration / subscription link.
 */
sealed class ConfigParseResult {
    data class Success(val server: ServerEntity) : ConfigParseResult()
    data class Failure(val reason: String) : ConfigParseResult()
}

/**
 * Parses common proxy share links (vless://, vmess://, trojan://, ss://,
 * hysteria2:// / hy2://) into a [ServerEntity] ready to be persisted.
 *
 * The parser intentionally keeps the dependency surface small: it only uses
 * the platform [Uri], [android.util.Base64] and the bundled org.json so it can
 * be exercised by plain JVM unit tests as well as the Android UI.
 */
object ConfigLinkParser {

    private const val DEFAULT_PORT = 443

    /**
     * Parse the supplied raw text. Leading/trailing whitespace and surrounding
     * quotes are removed. When the text contains more than one line the first
     * non-empty line is used, which makes pasting from chat clients forgiving.
     */
    fun parse(raw: String?): ConfigParseResult {
        val candidate = raw
            ?.lineSequence()
            ?.map { it.trim().trim('"', '\'') }
            ?.firstOrNull { it.isNotEmpty() }
            .orEmpty()

        if (candidate.isEmpty()) {
            return ConfigParseResult.Failure("Paste a configuration link to continue.")
        }

        return when {
            candidate.startsWith("vless://", ignoreCase = true) -> parseVless(candidate)
            candidate.startsWith("vmess://", ignoreCase = true) -> parseVmess(candidate)
            candidate.startsWith("trojan://", ignoreCase = true) -> parseTrojan(candidate)
            candidate.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(candidate)
            candidate.startsWith("hysteria2://", ignoreCase = true) ||
                candidate.startsWith("hy2://", ignoreCase = true) -> parseHysteria2(candidate)
            else -> ConfigParseResult.Failure(
                "Unsupported link. Supported schemes: vless, vmess, trojan, ss, hysteria2."
            )
        }
    }

    // region VLESS / Trojan / Hysteria2 (URI based)

    private fun parseVless(link: String): ConfigParseResult {
        val uri = runCatching { Uri.parse(link) }.getOrNull()
            ?: return ConfigParseResult.Failure("Malformed VLESS link.")

        val uuid = uri.userInfo.orEmpty()
        val host = uri.host.orEmpty()
        val port = uri.port.takeIf { it > 0 } ?: DEFAULT_PORT
        if (uuid.isBlank() || host.isBlank()) {
            return ConfigParseResult.Failure("VLESS link is missing UUID or host.")
        }

        val name = uri.fragment?.decode()?.takeIf { it.isNotBlank() }
            ?: fallbackAlias(host, port, "VLESS")

        val query = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
        val security = normalizeSecurity(query["security"])
        val transport = normalizeTransport(query["type"])
        val sni = query["sni"].orEmpty()
        val flow = query["flow"].orEmpty()
        val publicKey = query["pbk"].orEmpty()
        val shortId = query["sid"].orEmpty()
        val spiderX = query["spx"].orEmpty().ifBlank { "/" }

        return ConfigParseResult.Success(
            baseServer(
                alias = name,
                address = host,
                port = port,
                uuid = uuid,
                protocol = "VLESS",
                security = security
            ).copy(
                transport = transport,
                sni = sni,
                flow = flow,
                publicKey = publicKey,
                shortId = shortId,
                spiderX = spiderX
            )
        )
    }

    private fun parseTrojan(link: String): ConfigParseResult {
        val uri = runCatching { Uri.parse(link) }.getOrNull()
            ?: return ConfigParseResult.Failure("Malformed Trojan link.")

        val password = uri.userInfo.orEmpty()
        val host = uri.host.orEmpty()
        val port = uri.port.takeIf { it > 0 } ?: DEFAULT_PORT
        if (password.isBlank() || host.isBlank()) {
            return ConfigParseResult.Failure("Trojan link is missing password or host.")
        }

        val name = uri.fragment?.decode()?.takeIf { it.isNotBlank() }
            ?: fallbackAlias(host, port, "Trojan")
        val query = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
        val sni = query["sni"].orEmpty()

        return ConfigParseResult.Success(
            baseServer(
                alias = name,
                address = host,
                port = port,
                uuid = password,
                protocol = "Trojan",
                security = "TLS"
            ).copy(
                transport = normalizeTransport(query["type"]),
                sni = sni
            )
        )
    }

    private fun parseHysteria2(link: String): ConfigParseResult {
        val uri = runCatching { Uri.parse(link) }.getOrNull()
            ?: return ConfigParseResult.Failure("Malformed Hysteria2 link.")

        val password = uri.userInfo.orEmpty()
        val host = uri.host.orEmpty()
        val port = uri.port.takeIf { it > 0 } ?: DEFAULT_PORT
        if (password.isBlank() || host.isBlank()) {
            return ConfigParseResult.Failure("Hysteria2 link is missing password or host.")
        }

        val name = uri.fragment?.decode()?.takeIf { it.isNotBlank() }
            ?: fallbackAlias(host, port, "Hysteria2")
        val query = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
        val sni = query["sni"].orEmpty()
        val insecure = query["insecure"].equals("1", ignoreCase = true) ||
            query["insecure"].equals("true", ignoreCase = true)

        return ConfigParseResult.Success(
            baseServer(
                alias = name,
                address = host,
                port = port,
                uuid = password,
                protocol = "Hysteria2",
                security = if (insecure) "None" else "TLS"
            ).copy(
                transport = "UDP",
                sni = sni
            )
        )
    }

    // endregion

    // region VMess (base64 JSON)

    private fun parseVmess(link: String): ConfigParseResult {
        val payload = link.substringAfter("://")
        val decoded = decodeBase64Lenient(payload)
            ?: return ConfigParseResult.Failure("VMess link is not valid base64.")

        val json = runCatching { JSONObject(decoded) }.getOrNull()
            ?: return ConfigParseResult.Failure("VMess link payload is not valid JSON.")

        val host = json.optString("add").trim()
        val port = json.optString("port").trim().toIntOrNull()
            ?: return ConfigParseResult.Failure("VMess link has an invalid port.")
        val id = json.optString("id").trim()
        if (host.isBlank() || id.isBlank()) {
            return ConfigParseResult.Failure("VMess link is missing host or UUID.")
        }

        val alias = json.optString("ps").trim().ifBlank { fallbackAlias(host, port, "VMess") }
        val tls = json.optString("tls", "").lowercase()
        val security = when {
            tls == "reality" -> "Reality"
            tls.isNotBlank() -> "TLS"
            else -> "None"
        }
        val transport = normalizeTransport(json.optString("net").ifBlank { "tcp" })

        return ConfigParseResult.Success(
            baseServer(
                alias = alias,
                address = host,
                port = port,
                uuid = id,
                protocol = "VMess",
                security = security
            ).copy(
                transport = transport,
                sni = json.optString("sni").trim(),
                flow = json.optString("flow").trim(),
                publicKey = json.optString("pbk").trim(),
                shortId = json.optString("sid").trim()
            )
        )
    }

    // endregion

    // region Shadowsocks (SIP002 / legacy)

    private fun parseShadowsocks(link: String): ConfigParseResult {
        // Strip the scheme and split off the optional fragment / query.
        val withoutScheme = link.substringAfter("://")
        val fragmentPart = withoutScheme.substringAfterLast('#', "")
        val beforeFragment = withoutScheme.substringBeforeLast('#')

        val queryPart = beforeFragment.substringAfter('?', "")
        val main = beforeFragment.substringBefore('?')

        // SIP002: base64(method:password)@host:port
        val atIndex = main.lastIndexOf('@')
        val method: String
        val password: String
        val hostPort: String

        if (atIndex >= 0) {
            val encodedUserInfo = main.substring(0, atIndex)
            val decodedUserInfo = decodeBase64Lenient(encodedUserInfo)
                ?.takeIf { it.contains(':') }
                ?: encodedUserInfo // tolerate plain method:password
            val colonIndex = decodedUserInfo.indexOf(':')
            if (colonIndex <= 0) {
                return ConfigParseResult.Failure(
                    "Shadowsocks cipher and password are not separated by ':'."
                )
            }
            method = decodedUserInfo.substring(0, colonIndex)
            password = decodedUserInfo.substring(colonIndex + 1)
            hostPort = main.substring(atIndex + 1)
        } else {
            // Legacy format: base64(method:password@host:port)
            val decoded = decodeBase64Lenient(main)
                ?: return ConfigParseResult.Failure("Shadowsocks link is not valid base64.")
            val legacyAt = decoded.lastIndexOf('@')
            if (legacyAt < 0) {
                return ConfigParseResult.Failure("Shadowsocks link is missing host information.")
            }
            val userInfo = decoded.substring(0, legacyAt)
            val colonIndex = userInfo.indexOf(':')
            if (colonIndex < 0) {
                return ConfigParseResult.Failure("Shadowsocks cipher and password are not separated by ':'.")
            }
            method = userInfo.substring(0, colonIndex)
            password = userInfo.substring(colonIndex + 1)
            hostPort = decoded.substring(legacyAt + 1)
        }

        if (method.isBlank() || password.isBlank()) {
            return ConfigParseResult.Failure("Shadowsocks link is missing cipher or password.")
        }

        val host: String
        val port: Int
        if (hostPort.startsWith('[')) {
            // IPv6 literal: [::1]:443
            val endBracket = hostPort.indexOf(']')
            if (endBracket < 0) {
                return ConfigParseResult.Failure("Shadowsocks IPv6 host is malformed.")
            }
            host = hostPort.substring(1, endBracket)
            port = hostPort.substringAfter(':', "").toIntOrNull() ?: DEFAULT_PORT
        } else {
            val lastColon = hostPort.lastIndexOf(':')
            if (lastColon < 0) {
                host = hostPort
                port = DEFAULT_PORT
            } else {
                host = hostPort.substring(0, lastColon)
                port = hostPort.substring(lastColon + 1).toIntOrNull() ?: DEFAULT_PORT
            }
        }

        if (host.isBlank()) {
            return ConfigParseResult.Failure("Shadowsocks link is missing host.")
        }

        val alias = fragmentPart.decode().ifBlank { fallbackAlias(host, port, "Shadowsocks") }
        // plugin=obfs-local;obfs=tls;obfs-host=... (kept for future transport mapping)
        @Suppress("UNUSED_VARIABLE")
        val plugin = queryParameter(queryPart, "plugin")

        return ConfigParseResult.Success(
            baseServer(
                alias = alias,
                address = host,
                port = port,
                uuid = password,
                protocol = "Shadowsocks",
                security = "None"
            ).copy(
                method = method,
                transport = "TCP"
            )
        )
    }

    // endregion

    // region helpers

    private fun baseServer(
        alias: String,
        address: String,
        port: Int,
        uuid: String,
        protocol: String,
        security: String
    ) = ServerEntity(
        alias = alias,
        address = address,
        port = port,
        uuid = uuid,
        protocol = protocol,
        security = security,
        sni = if (security == "TLS" || security == "Reality") address else "",
        flow = if (protocol == "VLESS") "xtls-rprx-vision" else "",
        transport = "TCP",
        bestForRegionTag = "Imported config"
    )

    private fun fallbackAlias(host: String, port: Int, protocol: String): String {
        val compactHost = host.removePrefix("[").removeSuffix("]").substringBefore('.')
        return "$protocol $compactHost:$port"
    }

    private fun normalizeSecurity(value: String?): String = when (value?.trim()?.lowercase()) {
        "reality" -> "Reality"
        "tls" -> "TLS"
        "none", "" -> "None"
        null -> "None"
        else -> value.trim().replaceFirstChar { it.uppercase() }
    }

    private fun normalizeTransport(value: String?): String = when (value?.trim()?.lowercase()) {
        "tcp", "", null -> "TCP"
        "ws" -> "WS"
        "grpc" -> "gRPC"
        "xhttp" -> "XHTTP"
        "h2", "http" -> "H2"
        "kcp", "mkcp" -> "mKCP"
        "udp" -> "UDP"
        else -> value.trim().uppercase()
    }

    private fun queryParameter(query: String, key: String): String {
        if (query.isBlank()) return ""
        return query.split('&')
            .map { pair ->
                val idx = pair.indexOf('=')
                if (idx < 0) pair to "" else pair.substring(0, idx) to pair.substring(idx + 1)
            }
            .firstOrNull { it.first.equals(key, ignoreCase = true) }
            ?.second
            .orEmpty()
            .decode()
    }

    private fun String.decode(): String =
        runCatching { Uri.decode(this) }.getOrDefault(this)

    private fun decodeBase64Lenient(value: String): String? {
        val sanitized = value
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .trim()
            .let {
                // Restore padding if the source omitted it.
                val remainder = it.length % 4
                if (remainder == 0) it else it + "=".repeat(4 - remainder)
            }
        return runCatching {
            String(Base64.decode(sanitized, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
        }.recoverCatching {
            String(Base64.decode(sanitized, Base64.DEFAULT or Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    // endregion
}
