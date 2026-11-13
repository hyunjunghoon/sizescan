package io.github.hyunjunghoon.docscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/** Turns the pages of an existing PDF into page images, so it can be refitted to a size limit like a scan. */
object PdfPages {
    /** Pages beyond this are left out: upload limits are small, and 100 pages already take a while to fit. */
    const val MAX_PAGES = 100

    /** 200 dpi keeps small print readable after compression; the long side is capped like any other page. */
    private const val DPI = 200f
    private const val MAX_LONG_SIDE = 2480

    class PasswordProtected : IOException()

    /** Bitmap size for a page of [widthPt] x [heightPt] points (1/72 inch). */
    fun renderSize(widthPt: Int, heightPt: Int): Pair<Int, Int> {
        var scale = DPI / 72f
        val long = max(widthPt, heightPt) * scale
        if (long > MAX_LONG_SIDE) scale *= MAX_LONG_SIDE / long
        return max(1, (widthPt * scale).roundToInt()) to max(1, (heightPt * scale).roundToInt())
    }

    /** File name of the PDF without ".pdf", for naming the new document. */
    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()?.removeSuffix(".pdf")?.removeSuffix(".PDF")?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Renders up to [MAX_PAGES] pages of [uri] to JPEG files named by [fileFor] (called with 0, 1, 2…).
     * Returns the total page count of the PDF. Throws [PasswordProtected] for encrypted PDFs.
     */
    fun render(context: Context, uri: Uri, fileFor: (Int) -> File): Int {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw IOException("Cannot open")
        val renderer = try {
            PdfRenderer(pfd)
        } catch (e: SecurityException) {
            pfd.close()
            throw PasswordProtected()
        } catch (e: IOException) {
            pfd.close()
            throw e
        }
        renderer.use { r ->
            for (i in 0 until minOf(r.pageCount, MAX_PAGES)) {
                r.openPage(i).use { page ->
                    val (w, h) = renderSize(page.width, page.height)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    try {
                        // PDF pages are transparent where nothing is drawn; paper is white.
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        fileFor(i).outputStream().buffered().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    } finally {
                        bmp.recycle()
                    }
                }
            }
            return r.pageCount
        }
    }
}
