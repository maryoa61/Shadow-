package com.example.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreResolverTest {
    @Test
    fun `auto uses xray first for vless reality and keeps sing-box fallback`() {
        assertEquals(
            listOf(CoreEngine.XRAY, CoreEngine.SING_BOX),
            CoreResolver.candidates(CoreEngine.AUTO, "VLESS", "TCP")
        )
    }

    @Test
    fun `auto uses sing-box first for hysteria2`() {
        assertEquals(
            listOf(CoreEngine.SING_BOX, CoreEngine.XRAY),
            CoreResolver.candidates(CoreEngine.AUTO, "Hysteria2", "UDP")
        )
    }

    @Test
    fun `xhttp stays on xray because official sing-box does not implement it`() {
        assertEquals(
            listOf(CoreEngine.XRAY),
            CoreResolver.candidates(CoreEngine.AUTO, "VLESS", "XHTTP")
        )
    }
}
