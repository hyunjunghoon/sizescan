package io.github.hyunjunghoon.docscan.core

import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.Locale

data class JpegInfo(val width: Int, val height: Int, val components: Int)

object JpegInfoReader {
    /** Reads width, height and color components from the first SOF marker. */
    fun read(b: ByteArray): JpegInfo {
        require(b.size > 4 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte()) { "Not a JPEG" }
        var i = 2
        while (i + 3 < b.size) {
            if (b[i] != 0xFF.toByte()) { i++; continue }
            val marker = b[i + 1].toInt() and 0xFF
            if (marker == 0xFF) { i++; continue }
            if (marker == 0xD8 || marker == 0x01 || marker in 0xD0..0xD7) { i += 2; continue }
            val len = ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)
            val isSof = marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
            if (isSof) {
                require(i + 9 < b.size) { "Truncated SOF" }
                val h = ((b[i + 5].toInt() and 0xFF) shl 8) or (b[i + 6].toInt() and 0xFF)
                val w = ((b[i + 7].toInt() and 0xFF) shl 8) or (b[i + 8].toInt() and 0xFF)
                return JpegInfo(w, h, b[i + 9].toInt() and 0xFF)
            }
            i += 2 + len
        }
        throw IllegalArgumentException("No SOF marker")
    }
}

/**
 * Minimal PDF writer that embeds JPEG bytes as-is (DCTDecode), one image per page.
 * Because nothing is re-encoded, the PDF size is the sum of the JPEGs plus a small,
 * predictable overhead — which is what makes "fit under N KB" exact.
 */
object JpegPdfWriter {
    private const val LONG_SIDE_PT = 842.0 // A4 long side in points

    /** Upper bound of non-image bytes for a PDF with [pages] pages. */
    fun overheadBytes(pages: Int): Long = 600L + 560L * pages

    fun write(pages: List<ByteArray>, out: OutputStream) = write(pages.size, { pages[it] }, out)

    /** Streams pages one at a time via [page] so only one JPEG is in memory. */
    fun write(pageCount: Int, page: (Int) -> ByteArray, out: OutputStream) {
        require(pageCount > 0) { "No pages" }
        val w = CountingStream(out)
        val objCount = 2 + 3 * pageCount
        val offsets = LongArray(objCount + 1)
        fun ascii(s: String) = w.write(s.toByteArray(Charsets.US_ASCII))
        fun begin(n: Int) { offsets[n] = w.count; ascii("$n 0 obj\n") }

        ascii("%PDF-1.4\n")
        w.write(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        begin(1); ascii("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        val kids = (0 until pageCount).joinToString(" ") { "${3 + 3 * it} 0 R" }
        begin(2); ascii("<< /Type /Pages /Kids [$kids] /Count $pageCount >>\nendobj\n")

        for (k in 0 until pageCount) {
            val jpeg = page(k)
            val info = JpegInfoReader.read(jpeg)
            val scale = LONG_SIDE_PT / maxOf(info.width, info.height)
            val pw = fmt(info.width * scale)
            val ph = fmt(info.height * scale)
            val pageObj = 3 + 3 * k
            val imgObj = pageObj + 1
            val contentObj = pageObj + 2
            begin(pageObj)
            ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pw $ph] /Resources << /XObject << /Im0 $imgObj 0 R >> >> /Contents $contentObj 0 R >>\nendobj\n")
            val (cs, decode) = when (info.components) {
                1 -> "/DeviceGray" to ""
                4 -> "/DeviceCMYK" to " /Decode [1 0 1 0 1 0 1 0]"
                else -> "/DeviceRGB" to ""
            }
            begin(imgObj)
            ascii("<< /Type /XObject /Subtype /Image /Width ${info.width} /Height ${info.height} /ColorSpace $cs /BitsPerComponent 8$decode /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n")
            w.write(jpeg)
            ascii("\nendstream\nendobj\n")
            val content = "q $pw 0 0 $ph 0 0 cm /Im0 Do Q"
            begin(contentObj)
            ascii("<< /Length ${content.length} >>\nstream\n$content\nendstream\nendobj\n")
        }

        val xref = w.count
        ascii("xref\n0 ${objCount + 1}\n0000000000 65535 f \n")
        for (n in 1..objCount) ascii(String.format(Locale.US, "%010d 00000 n \n", offsets[n]))
        ascii("trailer\n<< /Size ${objCount + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        w.flush()
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)

    private class CountingStream(out: OutputStream) : FilterOutputStream(out) {
        var count = 0L
        override fun write(b: Int) { out.write(b); count++ }
        override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); count += len }
    }
}
