package io.github.hyunjunghoon.docscan.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfPagesTest {
    @Test
    fun a4RendersAt200Dpi() {
        // A4 = 595 x 842 pt → 8.27 x 11.69 in at 200 dpi.
        assertEquals(1653 to 2339, PdfPages.renderSize(595, 842))
    }

    @Test
    fun landscapeKeepsOrientation() {
        assertEquals(2200 to 1700, PdfPages.renderSize(792, 612))
    }

    @Test
    fun largePagesAreCappedAtTheWorkingResolution() {
        // A3 at 200 dpi would be 3308 px tall; pages are capped at 2480 px like scans.
        val (w, h) = PdfPages.renderSize(842, 1191)
        assertEquals(2480, h)
        assertEquals(1753, w)
    }

    @Test
    fun degeneratePagesStillGetAPixel() {
        assertEquals(1 to 1, PdfPages.renderSize(0, 0))
    }
}
