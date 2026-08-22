package com.example.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnTelemetryTest {
    @Test
    fun `small telemetry samples accumulate instead of rounding away`() {
        val usageAfterFiveSamples = (1..5).fold(4.2) { usage, _ ->
            nextTelemetryUsageGb(usage)
        }

        assertEquals(4.21, usageAfterFiveSamples, 0.000_001)
    }
}
