package io.github.hyunjunghoon.docscan.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class JpegPdfWriterTest {
    private fun jpeg(w: Int, h: Int): ByteArray {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until h) for (x in 0 until w) img.setRGB(x, y, (x * 7 xor y * 13) and 0xFFFFFF)
        return ByteArrayOutputStream().also { ImageIO.write(img, "jpg", it) }.toByteArray()
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
        val big = jpeg(1240, 1754)
        for (n in listOf(1, 2, 10, 40, 100)) {
            val pages = List(n) { big }
            val size = ByteArrayOutputStream().also { JpegPdfWriter.write(pages, it) }.size()
            val imageBytes = pages.sumOf { it.size }
            assertTrue("n=$n", size - imageBytes <= JpegPdfWriter.overheadBytes(n))
        }
    }
}
