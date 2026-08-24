package com.example.vpn

import android.util.Base64
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class XudpBaseKeyTest {
    @Test
    fun `generated key is raw url-safe base64 that decodes to exactly 32 bytes`() {
        val key = XudpBaseKey.generate()

        assertEquals(XudpBaseKey.RAW_URL_LENGTH, key.length)
        assertEquals(
            XudpBaseKey.KEY_BYTES,
            Base64.decode(key, Base64.URL_SAFE or Base64.NO_PADDING).size
        )
        assertTrue(XudpBaseKey.isValid(key))
    }

    @Test
    fun `each generated key is unique`() {
        assertNotEquals(XudpBaseKey.generate(), XudpBaseKey.generate())
    }

    @Test
    fun `isValid rejects blank wrong length and non url-safe keys`() {
        assertFalse(XudpBaseKey.isValid(null))
        assertFalse(XudpBaseKey.isValid(""))
        assertFalse(XudpBaseKey.isValid("   "))

        // Wrong decoded length (16 or 64 bytes instead of 32).
        assertFalse(XudpBaseKey.isValid(Base64.encodeToString(ByteArray(16), Base64.NO_WRAP)))
        assertFalse(XudpBaseKey.isValid(Base64.encodeToString(ByteArray(64), Base64.NO_WRAP)))

        // Standard Base64 of 32 bytes: Xray's RawURLEncoding rejects both its
        // '+'/'/' alphabet and its '=' padding.
        assertFalse(XudpBaseKey.isValid(Base64.encodeToString(ByteArray(32), Base64.NO_WRAP)))

        // URL-safe but padded: RawURLEncoding rejects padding too.
        assertFalse(
            XudpBaseKey.isValid(
                Base64.encodeToString(ByteArray(32), Base64.NO_WRAP or Base64.URL_SAFE)
            )
        )

        // Injecting a '+' into an otherwise well-formed key must fail.
        val wellFormed = XudpBaseKey.generate()
        val injected = wellFormed.replaceFirst(Regex("[A-Za-z0-9_-]"), "+")
        assertFalse(XudpBaseKey.isValid(injected))
    }

    @Test
    fun `regression uuid strings are rejected as xudp base keys`() {
        repeat(16) {
            assertFalse(XudpBaseKey.isValid(UUID.randomUUID().toString()))
        }
    }
}
