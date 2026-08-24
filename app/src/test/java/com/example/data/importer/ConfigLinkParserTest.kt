package com.example.data.importer

import android.util.Base64
import com.example.data.local.ServerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConfigLinkParserTest {

    @Test
    fun `parses vless reality link with pbk and sid`() {
        val link = "vless://11111111-2222-3333-4444-555555555555@example.com:443" +
            "?security=reality&sni=www.microsoft.com&fp=chrome&pbk=PbK_AbCdEf" +
            "&sid=01ab&type=tcp&flow=xtls-rprx-vision#DE-Frankfurt"

        val result = ConfigLinkParser.parse(link)
        assertTrue(result.toString(), result is ConfigParseResult.Success)
        val server = (result as ConfigParseResult.Success).server

        assertEquals("DE-Frankfurt", server.alias)
        assertEquals("VLESS", server.protocol)
        assertEquals("Reality", server.security)
        assertEquals("example.com", server.address)
        assertEquals(443, server.port)
        assertEquals("11111111-2222-3333-4444-555555555555", server.uuid)
        assertEquals("www.microsoft.com", server.sni)
        assertEquals("xtls-rprx-vision", server.flow)
        assertEquals("PbK_AbCdEf", server.publicKey)
        assertEquals("01ab", server.shortId)
        assertEquals("TCP", server.transport)
    }

    @Test
    fun `parses trojan tls link`() {
        val link = "trojan://SuperSecretPassword@trojan.example.com:8443" +
            "?sni=trojan.example.com&type=tcp#TR%20Istanbul"

        val result = ConfigLinkParser.parse(link)
        assertTrue(result is ConfigParseResult.Success)
        val server = (result as ConfigParseResult.Success).server

        assertEquals("TR Istanbul", server.alias)
        assertEquals("Trojan", server.protocol)
        assertEquals("TLS", server.security)
        assertEquals("trojan.example.com", server.address)
        assertEquals(8443, server.port)
        assertEquals("SuperSecretPassword", server.uuid)
    }

    @Test
    fun `parses base64 vmess json link`() {
        val json = """{
            "v":"2","ps":"SG-Singapore","add":"sg.example.com","port":"443",
            "id":"deadbeef-dead-beef-dead-beefdeadbeef","aid":"0",
            "net":"ws","type":"none","host":"","path":"/ws","tls":"tls",
            "sni":"sg.example.com"
        }""".trimIndent()
        val encoded = Base64.encodeToString(
            json.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val link = "vmess://$encoded"

        val result = ConfigLinkParser.parse(link)
        assertTrue(result is ConfigParseResult.Success)
        val server = (result as ConfigParseResult.Success).server

        assertEquals("SG-Singapore", server.alias)
        assertEquals("VMess", server.protocol)
        assertEquals("TLS", server.security)
        assertEquals("sg.example.com", server.address)
        assertEquals(443, server.port)
        assertEquals("deadbeef-dead-beef-dead-beefdeadbeef", server.uuid)
        assertEquals("WS", server.transport)
    }

    @Test
    fun `parses legacy base64 shadowsocks link`() {
        val userInfo = "aes-256-gcm:pa55word!@#"
        val legacy = Base64.encodeToString(
            "$userInfo@ss.example.com:8388".toByteArray(),
            Base64.NO_WRAP
        )
        val link = "ss://$legacy#NL%20Amsterdam"

        val result = ConfigLinkParser.parse(link)
        assertTrue(result is ConfigParseResult.Success)
        val server = (result as ConfigParseResult.Success).server

        assertEquals("NL Amsterdam", server.alias)
        assertEquals("Shadowsocks", server.protocol)
        assertEquals("ss.example.com", server.address)
        assertEquals(8388, server.port)
        assertEquals("aes-256-gcm", server.method)
        assertEquals("pa55word!@#", server.uuid)
    }

    @Test
    fun `parses sip002 shadowsocks link`() {
        val userInfo = Base64.encodeToString(
            "chacha20-ietf-poly1305:secret".toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val link = "ss://$userInfo@1.2.3.4:8388#US%20East"

        val result = ConfigLinkParser.parse(link)
        assertTrue(result is ConfigParseResult.Success)
        val server = (result as ConfigParseResult.Success).server

        assertEquals("US East", server.alias)
        assertEquals("1.2.3.4", server.address)
        assertEquals(8388, server.port)
        assertEquals("chacha20-ietf-poly1305", server.method)
        assertEquals("secret", server.uuid)
    }

    @Test
    fun `parses hysteria2 link`() {
        val link = "hysteria2://StrongHy2Password@hy2.example.com:443" +
            "?sni=hy2.example.com&insecure=0#AE-Dubai"

        val result = ConfigLinkParser.parse(link)
        assertTrue(result is ConfigParseResult.Success)
        val server = (result as ConfigParseResult.Success).server

        assertEquals("AE-Dubai", server.alias)
        assertEquals("Hysteria2", server.protocol)
        assertEquals("hy2.example.com", server.address)
        assertEquals(443, server.port)
        assertEquals("StrongHy2Password", server.uuid)
        assertEquals("UDP", server.transport)
    }

    @Test
    fun `empty input produces a failure`() {
        val result = ConfigLinkParser.parse("   \n  ")
        assertTrue(result is ConfigParseResult.Failure)
    }

    @Test
    fun `unknown scheme produces a failure`() {
        val result = ConfigLinkParser.parse("https://example.com/config")
        assertTrue(result is ConfigParseResult.Failure)
    }

    @Test
    fun `success result is a persistable server entity with sensible defaults`() {
        val link = "vless://uuid@host:443?security=none#node"
        val result = ConfigLinkParser.parse(link)
        assertTrue(result is ConfigParseResult.Success)
        val server: ServerEntity = (result as ConfigParseResult.Success).server
        assertNotNull(server.bestForRegionTag)
        assertEquals(0L, server.id)
    }
}
