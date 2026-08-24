package com.example.vpn

import android.util.Base64
import java.security.SecureRandom

/**
 * Generates and validates the XUDP base key passed to `Libv2ray.initCoreEnv`.
 *
 * Xray's `common/xudp` layer decodes `xray.xudp.basekey` with Go's
 * `base64.RawURLEncoding` (URL-safe alphabet `-_`, no padding) and requires the
 * decoded payload to be exactly 32 bytes:
 *
 * ```go
 * key, _ := base64.RawURLEncoding.DecodeString(raw)
 * if len(key) != 32 { return errors.New("... BaseKey must be 32 bytes ...") }
 * ```
 *
 * The app previously passed a 36-character UUID string, which that encoding
 * decodes to only 27 bytes (and even standard `Base64.NO_WRAP` output would be
 * rejected because of its `+`, `/`, and `=` characters), aborting every
 * connection. The key therefore must be generated as *raw URL-safe* Base64.
 */
object XudpBaseKey {
    const val KEY_BYTES = 32

    /** Raw URL-safe Base64 of [KEY_BYTES] bytes has no padding: always 43 chars. */
    const val RAW_URL_LENGTH = (KEY_BYTES * 4 + 2) / 3

    private val secureRandom = SecureRandom()

    private val RAW_URL_PATTERN = Regex("[A-Za-z0-9_-]{$RAW_URL_LENGTH}")

    /**
     * Encodes 32 cryptographically-random bytes as raw URL-safe Base64
     * (no line breaks, no padding), matching `base64.RawURLEncoding`.
     */
    fun generate(): String {
        val bytes = ByteArray(KEY_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    /**
     * True only when [value] is exactly what `base64.RawURLEncoding` accepts
     * for a 32-byte key: 43 URL-safe characters (`A-Z a-z 0-9 - _`, no
     * padding) that decode to [KEY_BYTES] bytes.
     */
    fun isValid(value: String?): Boolean {
        if (value == null) return false
        if (!RAW_URL_PATTERN.matches(value)) return false
        return runCatching {
            Base64.decode(value, Base64.URL_SAFE or Base64.NO_PADDING).size == KEY_BYTES
        }.getOrDefault(false)
    }
}
