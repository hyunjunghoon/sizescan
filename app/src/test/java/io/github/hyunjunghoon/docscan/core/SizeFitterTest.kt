package io.github.hyunjunghoon.docscan.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeFitterTest {
    /** Fake encoder: size grows with pixel count and quality, like real JPEG. */
    private class FakeEncoder(private val bytesAtFull: Int) : PageEncoder {
        var calls = 0
        override fun encode(scale: Float, quality: Int): ByteArray {
            calls++
            val q = 0.15 + 0.85 * (quality / 100.0)
            return ByteArray(maxOf(200, (bytesAtFull * scale * scale * q).toInt()))
        }
    }

    @Test
    fun generousBudgetKeepsFullScaleAndMaxQuality() {
        val r = SizeFitter().fitSingle(FakeEncoder(100_000), 10_000_000)
        assertTrue(r.reached)
        assertEquals(1f, r.scale)
        assertEquals(92, r.quality)
    }

    @Test
    fun resultNeverExceedsBudget() {
        for (budget in listOf(20_000L, 50_000L, 100_000L, 200_000L, 333_333L)) {
            val r = SizeFitter().fitSingle(FakeEncoder(1_500_000), budget)
            assertTrue("budget=$budget", r.reached)
            assertTrue("budget=$budget size=${r.bytes.size}", r.bytes.size <= budget)
        }
    }

    @Test
    fun prefersReadableQualityOverTinyResolution() {
        val r = SizeFitter().fitSingle(FakeEncoder(1_500_000), 200_000)
        assertTrue(r.quality >= 55)
    }

    @Test
    fun impossibleBudgetReturnsSmallestAndFlagsIt() {
        val r = SizeFitter().fitSingle(FakeEncoder(1_500_000), 100)
        assertFalse(r.reached)
        assertEquals(200, r.bytes.size)
    }

    @Test
    fun fitManyStaysWithinSharedBudget() {
        val encoders = listOf(FakeEncoder(2_000_000), FakeEncoder(300_000), FakeEncoder(900_000))
        val budget = 150_000L
        val results = SizeFitter().fitMany(encoders, budget)
        assertTrue(results.all { it.reached })
        assertTrue(results.sumOf { it.bytes.size.toLong() } <= budget)
    }

    @Test
    fun encodeCallsStayBounded() {
        val enc = FakeEncoder(5_000_000)
        SizeFitter().fitSingle(enc, 30_000)
        assertTrue("calls=${enc.calls}", enc.calls < 40)
    }
}
