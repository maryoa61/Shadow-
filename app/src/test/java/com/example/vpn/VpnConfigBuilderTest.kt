package com.example.vpn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ServerEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VpnConfigBuilderTest {
    @Suppress("unused")
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `xray reality config contains the real selected endpoint and tun inbound`() {
        val json = JSONObject(VpnConfigBuilder.buildXray(realityServer(), "Chrome"))

        assertEquals("tun", json.getJSONArray("inbounds").getJSONObject(0).getString("protocol"))
        val outbound = json.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("vless", outbound.getString("protocol"))
        assertEquals("vpn.example.com", outbound.getJSONObject("settings").getString("address"))
        assertEquals(
            "public-key",
            outbound.getJSONObject("streamSettings")
                .getJSONObject("realitySettings")
                .getString("publicKey")
        )
    }

    @Test
    fun `sing-box mode exposes only a loopback proxy for the xray tun bridge`() {
        val singBox = JSONObject(VpnConfigBuilder.buildSingBox(realityServer(), "Firefox"))
        val inbound = singBox.getJSONArray("inbounds").getJSONObject(0)
        assertEquals("mixed", inbound.getString("type"))
        assertEquals("127.0.0.1", inbound.getString("listen"))
        assertFalse(inbound.has("auto_route"))

        val bridge = JSONObject(VpnConfigBuilder.buildXrayToSingBoxBridge())
        assertEquals(
            "socks",
            bridge.getJSONArray("outbounds").getJSONObject(0).getString("protocol")
        )
    }

    @Test
    fun `demo host and incomplete shadowsocks profiles are rejected`() {
        val demo = realityServer().copy(address = "fra-01.shadownet.core")
        assertTrue(VpnConfigBuilder.validate(demo, CoreEngine.XRAY)!!.contains("demo"))

        val shadowsocks = realityServer().copy(
            protocol = "Shadowsocks",
            security = "None",
            publicKey = "",
            sni = "",
            method = ""
        )
        assertTrue(VpnConfigBuilder.validate(shadowsocks, CoreEngine.SING_BOX)!!.contains("cipher"))
        assertNull(
            VpnConfigBuilder.validate(
                shadowsocks.copy(method = "chacha20-ietf-poly1305"),
                CoreEngine.SING_BOX
            )
        )
    }

    private fun realityServer() = ServerEntity(
        alias = "Real node",
        address = "vpn.example.com",
        port = 443,
        uuid = "12345678-abcd-ef01-2345-6789abcdef01",
        protocol = "VLESS",
        security = "Reality",
        publicKey = "public-key",
        shortId = "0123456789abcdef",
        spiderX = "/",
        sni = "www.cloudflare.com",
        flow = "xtls-rprx-vision",
        transport = "TCP"
    )
}
