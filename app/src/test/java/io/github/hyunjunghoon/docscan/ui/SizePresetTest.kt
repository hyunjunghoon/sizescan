package io.github.hyunjunghoon.docscan.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class SizePresetTest {
    @Test
    fun presetsAreValidRanges() {
        SIZE_PRESETS.forEach { p ->
            assertTrue(p.maxKb in 5..50_000)
            // Same rule as the custom dialog and Exporter: min counts 1 KB as 1024 bytes, max as 1000.
            p.minKb?.let { assertTrue("${p.minKb}-${p.maxKb}", it * 1024L < p.maxKb * 1000L) }
        }
    }
}
