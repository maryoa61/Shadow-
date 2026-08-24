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
    fun `generated key is base64 that decodes to exactly 32 bytes`() {
        val key = XudpBaseKey.generate()

        assertEquals(XudpBaseKey.KEY_BYTES, Base64.decode(key, Base64.NO_WRAP).size)
        assertTrue(XudpBaseKey.isValid(key))
    }

    @Test
    fun `each generated key is unique`() {
        assertNotEquals(XudpBaseKey.generate(), XudpBaseKey.generate())
    }

    @Test
    fun `isValid rejects blank and wrong length keys`() {
        assertFalse(XudpBaseKey.isValid(null))
        assertFalse(XudpBaseKey.isValid(""))
        assertFalse(XudpBaseKey.isValid("   "))
        // Base64 of 16 bytes: valid Base64, but not the required 32.
        assertFalse(XudpBaseKey.isValid(Base64.encodeToString(ByteArray(16), Base64.NO_WRAP)))
        // Base64 of 64 bytes: valid Base64, but again not 32.
        assertFalse(XudpBaseKey.isValid(Base64.encodeToString(ByteArray(64), Base64.NO_WRAP)))
    }

    @Test
    fun `regression uuid strings are rejected as xudp base keys`() {
        repeat(16) {
            assertFalse(XudpBaseKey.isValid(UUID.randomUUID().toString()))
        }
    }
}
