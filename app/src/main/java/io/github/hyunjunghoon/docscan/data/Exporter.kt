package io.github.hyunjunghoon.docscan.data

import android.content.Context
import io.github.hyunjunghoon.docscan.core.JpegPdfWriter
import io.github.hyunjunghoon.docscan.core.SizeFitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ExportFormat { PDF, JPG }

data class ExportResult(
    val files: List<File>,
    val totalBytes: Long,
    /** Size limit in bytes (per file for JPG), or null for original quality. */
    val limitBytes: Long?,
    val reached: Boolean,
    val format: ExportFormat,
)

class Exporter(private val context: Context) {
    private val outDir get() = File(context.cacheDir, "exports")

    suspend fun export(
        doc: Doc,
        format: ExportFormat,
        limitKb: Int?,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ExportResult = withContext(Dispatchers.Default) {
        require(doc.pages.isNotEmpty())
        outDir.deleteRecursively()
        outDir.mkdirs()
        val base = FileNames.safe(doc.name)
        // 1 KB = 1000 bytes, so the file also passes portals that count 1 KB as 1024 bytes.
        val limit = limitKb?.let { it * 1000L }
        val fitter = SizeFitter()
        when (format) {
            ExportFormat.PDF -> {
                val file = File(outDir, "$base.pdf")
                if (limit == null) {
                    file.outputStream().buffered().use { out ->
                        JpegPdfWriter.write(doc.pages.size, { doc.pages[it].readBytes() }, out)
                    }
                    return@withContext ExportResult(listOf(file), file.length(), null, true, format)
                }
                var budget = (limit - JpegPdfWriter.overheadBytes(doc.pages.size)).coerceAtLeast(1)
                var reached = false
                for (attempt in 0 until 3) {
                    val results = fitter.fitMany(doc.pages.map { AndroidPageEncoder(it) }, budget, onProgress)
                    file.outputStream().buffered().use { JpegPdfWriter.write(results.map { it.bytes }, it) }
                    val over = file.length() - limit
                    val fitted = results.all { it.reached }
                    reached = fitted && over <= 0
                    if (reached || !fitted) break
                    // Safety net in case the overhead estimate was too small: retry with a tighter budget.
                    budget = (budget - over - 512).coerceAtLeast(1)
                }
                ExportResult(listOf(file), file.length(), limit, reached, format)
            }
            ExportFormat.JPG -> {
                var reached = true
                val files = doc.pages.mapIndexed { i, page ->
                    onProgress(i, doc.pages.size)
                    val name = if (doc.pages.size == 1) "$base.jpg" else "${base}_${i + 1}.jpg"
                    val out = File(outDir, name)
                    if (limit == null) {
                        page.copyTo(out, overwrite = true)
                    } else {
                        val enc = AndroidPageEncoder(page)
                        val r = try { fitter.fitSingle(enc, limit) } finally { enc.release() }
                        if (!r.reached) reached = false
                        out.writeBytes(r.bytes)
                    }
                    out
                }
                onProgress(doc.pages.size, doc.pages.size)
                ExportResult(files, files.sumOf { it.length() }, limit, reached, format)
            }
        }
    }
}

object FileNames {
    private val illegal = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun safe(name: String): String =
        name.replace(illegal, "_").trim().trim('.').take(80).ifBlank { "scan" }
}
