package io.github.hyunjunghoon.docscan.data

import android.content.Context
import io.github.hyunjunghoon.docscan.core.JpegPadding
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
    /** Lower size bound in bytes (per file for JPG), or null when only a maximum was asked for. */
    val minBytes: Long? = null,
)

class Exporter(private val context: Context) {
    private val outDir get() = File(context.cacheDir, "exports")

    suspend fun export(
        doc: Doc,
        format: ExportFormat,
        limitKb: Int?,
        minKb: Int? = null,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ExportResult = withContext(Dispatchers.Default) {
        require(doc.pages.isNotEmpty())
        outDir.deleteRecursively()
        outDir.mkdirs()
        val base = FileNames.safe(doc.name)
        // 1 KB = 1000 bytes, so the file also passes portals that count 1 KB as 1024 bytes.
        val limit = limitKb?.let { it * 1000L }
        // The minimum uses 1 KB = 1024 bytes for the same reason, so both ways of counting are met.
        val min = if (limit != null) minKb?.let { it * 1024L }?.takeIf { it < limit } else null
        val fitter = SizeFitter()
        when (format) {
            ExportFormat.PDF -> {
                val file = File(outDir, "$base.pdf")
                if (limit == null) {
                    file.outputStream().buffered().use { out ->
                        JpegPdfWriter.write(doc.pages.size, { i -> onProgress(i, doc.pages.size); doc.pages[i].readBytes() }, out)
                    }
                    return@withContext ExportResult(listOf(file), file.length(), null, true, format)
                }
                val overhead = JpegPdfWriter.overheadBytes(doc.pages.size)
                var budget = (limit - overhead).coerceAtLeast(1)
                val minBudget = ((min ?: 0) - overhead).coerceAtLeast(0)
                var reached = false
                for (attempt in 0 until 3) {
                    val results = fitter.fitMany(doc.pages.map { AndroidPageEncoder(it) }, budget, minBudget, onProgress)
                    val pages = results.map { it.bytes }.toMutableList()
                    file.outputStream().buffered().use { JpegPdfWriter.write(pages, it) }
                    if (min != null && file.length() < min) {
                        // The overhead estimate was too large: top up the last page by the difference.
                        pages[pages.lastIndex] = JpegPadding.pad(pages.last(), pages.last().size + min - file.length())
                        file.outputStream().buffered().use { JpegPdfWriter.write(pages, it) }
                    }
                    val over = file.length() - limit
                    val fitted = results.all { it.reached }
                    reached = fitted && over <= 0 && file.length() >= (min ?: 0)
                    if (reached || !fitted) break
                    // Safety net in case the overhead estimate was too small: retry with a tighter budget.
                    budget = (budget - over - 512).coerceAtLeast(1)
                }
                ExportResult(listOf(file), file.length(), limit, reached, format, min)
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
                        val r = try { fitter.fitSingle(enc, limit, min ?: 0) } finally { enc.release() }
                        if (!r.reached) reached = false
                        out.writeBytes(r.bytes)
                    }
                    out
                }
                onProgress(doc.pages.size, doc.pages.size)
                ExportResult(files, files.sumOf { it.length() }, limit, reached, format, min)
            }
        }
    }
}

object FileNames {
    private val illegal = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun safe(name: String): String =
        name.replace(illegal, "_").trim().trim('.').take(80).ifBlank { "scan" }
}
