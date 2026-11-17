package io.github.hyunjunghoon.docscan

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.hyunjunghoon.docscan.data.Doc
import io.github.hyunjunghoon.docscan.data.DocRepository
import io.github.hyunjunghoon.docscan.data.ExportFormat
import io.github.hyunjunghoon.docscan.data.Exporter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Real-device flow on an emulator (README rule 11, step 2): start the app, share a 22-page PDF into it,
 * and fit the result under common upload limits. Runs the same code as the store build with test ad IDs.
 */
@RunWith(AndroidJUnit4::class)
class DeviceFlowTest {
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo get() = DocRepository(ctx)

    private fun makePdf(name: String, pages: Int): Uri {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$name.pdf")
        val pdf = PdfDocument()
        val text = Paint().apply { textSize = 13f; color = Color.BLACK }
        val bar = Paint().apply { color = Color.rgb(26, 115, 232) }
        repeat(pages) { i ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create())
            page.canvas.drawRect(40f, 40f, 300f, 70f, bar)
            repeat(40) { l ->
                page.canvas.drawText("Page ${i + 1}, line ${l + 1}: candidate details, address and declaration text", 40f, 100f + l * 17, text)
            }
            pdf.finishPage(page)
        }
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
    }

    private fun <T : Any> waitFor(timeoutMs: Long, get: () -> T?): T {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            get()?.let { return it }
            Thread.sleep(250)
        }
        throw AssertionError("Timed out after $timeoutMs ms")
    }

    @Test
    fun appStartsNormally() {
        ActivityScenario.launch(MainActivity::class.java).use { sc ->
            Thread.sleep(3000)
            assertEquals(Lifecycle.State.RESUMED, sc.state)
        }
    }

    @Test
    fun sharedPdfIsImportedAndFitsUnder200Kb() {
        val before = repo.list().map { it.id }.toSet()
        val uri = makePdf("Admission form", 22)
        val intent = Intent(Intent.ACTION_SEND)
            .setClass(ctx, MainActivity::class.java)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ActivityScenario.launch<MainActivity>(intent).use { sc ->
            val doc = waitFor(90_000) { repo.list().firstOrNull { it.id !in before && it.pages.size == 22 } }
            assertEquals("Admission form", doc.name)

            val pdf = runBlocking { Exporter(ctx).export(doc, ExportFormat.PDF, 200, null) { _, _ -> } }
            assertTrue("PDF ${pdf.totalBytes} bytes", pdf.reached && pdf.totalBytes <= 200_000)

            // 50-200 KB window, as some portals require.
            val ranged = runBlocking { Exporter(ctx).export(doc, ExportFormat.PDF, 200, 50) { _, _ -> } }
            assertTrue("PDF ${ranged.totalBytes} bytes", ranged.reached && ranged.totalBytes in 51_200..200_000)

            // A single page as a signature-size JPG.
            val one = Doc(doc.id, "Signature", doc.createdAt, listOf(doc.pages.first()))
            val jpg = runBlocking { Exporter(ctx).export(one, ExportFormat.JPG, 20, 10) { _, _ -> } }
            assertTrue("JPG ${jpg.totalBytes} bytes", jpg.reached && jpg.totalBytes in 10_240..20_000)

            assertEquals(Lifecycle.State.RESUMED, sc.state)
            repo.delete(doc.id)
        }
    }

    @Test
    fun sharedImagesBecomeOneDocument() {
        val before = repo.list().map { it.id }.toSet()
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val uris = ArrayList<Uri>()
        repeat(3) { i ->
            val bmp = android.graphics.Bitmap.createBitmap(1200, 1600, android.graphics.Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.rgb(240 - i * 20, 240, 250))
            val f = File(dir, "photo$i.jpg")
            f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
            uris += FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            .setClass(ctx, MainActivity::class.java)
            .setType("image/jpeg")
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ActivityScenario.launch<MainActivity>(intent).use {
            val doc = waitFor(60_000) { repo.list().firstOrNull { it.id !in before && it.pages.size == 3 } }
            assertNotNull(doc)
            repo.delete(doc.id)
        }
    }
}
