package com.example.vpn

import android.util.Base64
import java.security.SecureRandom

/**
 * Generates and validates the XUDP base key passed to `Libv2ray.initCoreEnv`.
 *
 * Xray's `common/xudp` layer requires `xray.xudp.basekey` to be a Base64 string
 * whose decoded payload is exactly 32 bytes. Passing anything else (e.g. the
 * 36-character UUID string this app used to send, which decodes to 27 bytes)
 * aborts every connection with "BaseKey must be 32 bytes".
 */
object XudpBaseKey {
    const val KEY_BYTES = 32

    private val secureRandom = SecureRandom()

    /** Encodes 32 cryptographically-random bytes as URL-unsafe, unwrapped Base64. */
    fun generate(): String {
        val bytes = ByteArray(KEY_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** True only when [value] is Base64 that decodes to exactly [KEY_BYTES] bytes. */
    fun isValid(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return runCatching {
            Base64.decode(value, Base64.NO_WRAP).size == KEY_BYTES
        }.getOrDefault(false)
    }
}
