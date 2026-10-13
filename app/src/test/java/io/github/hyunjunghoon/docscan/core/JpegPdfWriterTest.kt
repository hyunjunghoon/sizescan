package io.github.hyunjunghoon.docscan.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class JpegPdfWriterTest {
    /**
     * Builds a structurally valid JPEG header (SOI, APP0, SOF0, EOI) with [w]x[h] and filler
     * bytes. java.awt is not on the Android unit-test classpath, and the writer only reads SOF.
     */
    private fun jpeg(w: Int, h: Int, filler: Int = 64): ByteArray {
        val out = ByteArrayOutputStream()
        fun bytes(vararg v: Int) = v.forEach { out.write(it) }
        bytes(0xFF, 0xD8)
        bytes(0xFF, 0xE0, 0x00, 0x10, 'J'.code, 'F'.code, 'I'.code, 'F'.code, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0)
        bytes(0xFF, 0xC0, 0x00, 0x11, 8, h shr 8, h and 0xFF, w shr 8, w and 0xFF, 3)
        bytes(1, 0x22, 0, 2, 0x11, 1, 3, 0x11, 1)
        repeat(filler) { out.write(0x55) }
        bytes(0xFF, 0xD9)
        return out.toByteArray()
    }

    @Test
    fun readsJpegDimensions() {
        val info = JpegInfoReader.read(jpeg(120, 80))
        assertEquals(120, info.width)
        assertEquals(80, info.height)
        assertEquals(3, info.components)
    }

    @Test
    fun writesValidPdfWithCorrectXref() {
        val pages = listOf(jpeg(200, 300), jpeg(300, 200), jpeg(50, 50))
        val bytes = ByteArrayOutputStream().also { JpegPdfWriter.write(pages, it) }.toByteArray()
        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.trimEnd().endsWith("%%EOF"))

        val startXref = text.substringAfterLast("startxref\n").substringBefore("\n").trim().toInt()
        assertTrue(text.startsWith("xref", startXref))
        val entries = text.substring(startXref).lines().drop(3).take(2 + 3 * pages.size)
        entries.forEachIndexed { i, line ->
            val off = line.substring(0, 10).toInt()
            assertTrue("obj ${i + 1}", text.startsWith("${i + 1} 0 obj", off))
        }
        assertTrue(text.contains("/Count 3"))
    }

    @Test
    fun overheadEstimateIsAnUpperBound() {
        val big = jpeg(1240, 1754, filler = 200_000)
        for (n in listOf(1, 2, 10, 40, 100)) {
            val pages = List(n) { big }
            val size = ByteArrayOutputStream().also { JpegPdfWriter.write(pages, it) }.size()
            val imageBytes = pages.sumOf { it.size }
            assertTrue("n=$n", size - imageBytes <= JpegPdfWriter.overheadBytes(n))
        }
    }
}
