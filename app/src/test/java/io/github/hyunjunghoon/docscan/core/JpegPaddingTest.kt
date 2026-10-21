package io.github.hyunjunghoon.docscan.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class JpegPaddingTest {
    /** Minimal JPEG-shaped bytes: SOI, a baseline SOF0 for 30x20 RGB, EOI. */
    private val jpeg = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(),
        0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08, 0x00, 0x14, 0x00, 0x1E, 0x03,
        0x01, 0x22, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01,
        0xFF.toByte(), 0xD9.toByte(),
    )

    @Test
    fun padsToTargetAndKeepsImageReadable() {
        for (target in listOf(jpeg.size + 1L, jpeg.size + 4L, 10_240L, 65_540L, 65_541L, 65_561L, 200_000L)) {
            val out = JpegPadding.pad(jpeg, target)
            assertEquals("target=$target", true, out.size >= target && out.size <= target + 3)
            val info = JpegInfoReader.read(out)
            assertEquals(30, info.width)
            assertEquals(20, info.height)
            assertArrayEquals(jpeg.copyOfRange(2, jpeg.size), out.copyOfRange(out.size - jpeg.size + 2, out.size))
        }
    }

    @Test
    fun noPaddingWhenAlreadyLargeEnough() {
        assertArrayEquals(jpeg, JpegPadding.pad(jpeg, 10))
    }
}
