package io.github.hyunjunghoon.docscan.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.Locale
import java.util.Properties
import java.util.UUID

data class Doc(val id: String, val name: String, val createdAt: Long, val pages: List<File>)

/** Documents live in app-private storage: files/docs/<id>/meta.properties + p0001.jpg... */
class DocRepository(private val context: Context) {
    private val root = File(context.filesDir, "docs").apply { mkdirs() }

    fun list(): List<Doc> =
        root.listFiles()?.mapNotNull { load(it) }?.sortedByDescending { it.createdAt } ?: emptyList()

    fun get(id: String): Doc? = load(File(root, id))

    fun create(name: String): String {
        val id = UUID.randomUUID().toString()
        val dir = File(root, id).apply { mkdirs() }
        writeMeta(dir, name, System.currentTimeMillis())
        return id
    }

    /** Adds images as pages. Returns how many could not be imported. */
    fun addPages(id: String, uris: List<Uri>, fromScanner: Boolean): Int {
        val dir = File(root, id)
        var next = (pageFiles(dir).maxOfOrNull { it.name.substring(1, 5).toIntOrNull() ?: 0 } ?: 0) + 1
        var failed = 0
        for (uri in uris) {
            val out = File(dir, String.format(Locale.ROOT, "p%04d.jpg", next))
            val ok = runCatching {
                if (fromScanner) {
                    // Scanner output is already an upright JPEG.
                    context.contentResolver.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
                } else {
                    Images.normalizeToJpeg(context, uri, out)
                }
            }.isSuccess
            if (ok) next++ else { out.delete(); failed++ }
        }
        return failed
    }

    fun rename(id: String, name: String) {
        val doc = get(id) ?: return
        writeMeta(File(root, id), name, doc.createdAt)
    }

    fun delete(id: String) {
        File(root, id).deleteRecursively()
    }

    /** Deletes one page; deletes the whole document when it was the last page. */
    fun deletePage(id: String, page: File) {
        if (page.parentFile?.name != id) return
        page.delete()
        if (pageFiles(File(root, id)).isEmpty()) delete(id)
    }

    private fun pageFiles(dir: File): List<File> =
        dir.listFiles { f -> f.name.matches(PAGE_NAME) }?.sortedBy { it.name } ?: emptyList()

    private fun load(dir: File): Doc? {
        val meta = File(dir, META)
        if (!dir.isDirectory || !meta.exists()) return null
        val p = Properties().apply { meta.inputStream().use { load(it) } }
        return Doc(
            id = dir.name,
            name = p.getProperty("name").orEmpty(),
            createdAt = p.getProperty("created")?.toLongOrNull() ?: dir.lastModified(),
            pages = pageFiles(dir),
        )
    }

    private fun writeMeta(dir: File, name: String, created: Long) {
        val p = Properties().apply {
            setProperty("name", name)
            setProperty("created", created.toString())
        }
        File(dir, META).outputStream().use { p.store(it, null) }
    }

    companion object {
        private const val META = "meta.properties"
        private val PAGE_NAME = Regex("p\\d{4}\\.jpg")
    }
}
