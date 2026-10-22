package io.github.hyunjunghoon.docscan

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hyunjunghoon.docscan.core.SaveLocation
import io.github.hyunjunghoon.docscan.data.Doc
import io.github.hyunjunghoon.docscan.data.DocRepository
import io.github.hyunjunghoon.docscan.data.ExportFormat
import io.github.hyunjunghoon.docscan.data.ExportResult
import io.github.hyunjunghoon.docscan.data.Exporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import io.github.hyunjunghoon.docscan.ui.ThemeMode
import io.github.hyunjunghoon.docscan.ui.formatSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ExportUi {
    data object Hidden : ExportUi
    data class Running(val done: Int, val total: Int) : ExportUi
    data class Done(val result: ExportResult, val showAd: Boolean) : ExportUi
}

/** Snackbar text: a string resource plus already-formatted arguments. */
data class UiMessage(val res: Int, val args: List<String> = emptyList(), val long: Boolean = false)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DocRepository(app)
    private val exporter = Exporter(app)
    private val prefs = app.getSharedPreferences("app", Context.MODE_PRIVATE)
    private val importLock = Mutex()

    private val _docs = MutableStateFlow<List<Doc>>(emptyList())
    val docs: StateFlow<List<Doc>> = _docs
    private val _openDocId = MutableStateFlow<String?>(null)
    val openDocId: StateFlow<String?> = _openDocId
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _export = MutableStateFlow<ExportUi>(ExportUi.Hidden)
    val export: StateFlow<ExportUi> = _export
    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages
    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null)!!) }.getOrDefault(ThemeMode.SYSTEM),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    /** True until the how-to guide has been shown once, so it opens by itself on first launch. */
    val helpPending: Boolean
        get() = !prefs.getBoolean(KEY_HELP_SEEN, false)

    fun markHelpSeen() {
        prefs.edit().putBoolean(KEY_HELP_SEEN, true).apply()
    }

    val lastFormat: ExportFormat
        get() = runCatching { ExportFormat.valueOf(prefs.getString(KEY_FORMAT, null)!!) }.getOrDefault(ExportFormat.PDF)
    val lastLimitKb: Int?
        get() = prefs.getInt(KEY_LIMIT, 0).takeIf { it > 0 }
    val lastMinKb: Int?
        get() = prefs.getInt(KEY_MIN, 0).takeIf { it > 0 }

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch { _docs.value = withContext(Dispatchers.IO) { repo.list() } }
    }

    fun message(res: Int) {
        _messages.tryEmit(UiMessage(res))
    }

    fun openDoc(id: String?) {
        _openDocId.value = id
    }

    fun importImages(uris: List<Uri>, intoDocId: String?, fromScanner: Boolean) {
        viewModelScope.launch {
            // Serialized so two imports into one document can't pick the same page numbers.
            importLock.withLock { doImport(uris, intoDocId, fromScanner) }
        }
    }

    private suspend fun doImport(uris: List<Uri>, intoDocId: String?, fromScanner: Boolean) {
        _busy.value = true
        try {
            val id = withContext(Dispatchers.IO) {
                val target = intoDocId?.takeIf { repo.get(it) != null } ?: repo.create(defaultName())
                val failed = repo.addPages(target, uris, fromScanner)
                if (failed > 0) _messages.tryEmit(UiMessage(R.string.import_failed))
                if (repo.get(target)?.pages.isNullOrEmpty()) {
                    repo.delete(target)
                    null
                } else target
            }
            _docs.value = withContext(Dispatchers.IO) { repo.list() }
            // Don't pull the user back into a document they already left.
            if (id != null && (_openDocId.value == null || _openDocId.value == id)) _openDocId.value = id
        } finally {
            _busy.value = false
        }
    }

    fun rename(id: String, name: String) = io {
        val clean = name.trim()
        if (clean.isNotEmpty()) repo.rename(id, clean)
    }

    fun deleteDoc(id: String) {
        _openDocId.value = null
        io { repo.delete(id) }
    }

    fun rotatePage(id: String, page: File) = busyIo { repo.rotatePage(id, page) }

    fun movePage(id: String, page: File, by: Int) = busyIo { repo.movePage(id, page, by) }

    fun deletePage(id: String, page: File) = io {
        repo.deletePage(id, page)
        if (repo.get(id) == null) _openDocId.value = null
    }

    fun export(doc: Doc, format: ExportFormat, limitKb: Int?, minKb: Int?) {
        prefs.edit().putString(KEY_FORMAT, format.name).putInt(KEY_LIMIT, limitKb ?: 0).putInt(KEY_MIN, minKb ?: 0).apply()
        viewModelScope.launch {
            _export.value = ExportUi.Running(0, doc.pages.size)
            try {
                val result = exporter.export(doc, format, limitKb, minKb) { done, total ->
                    _export.value = ExportUi.Running(done, total)
                }
                val count = prefs.getInt(KEY_EXPORTS, 0) + 1
                prefs.edit().putInt(KEY_EXPORTS, count).apply()
                // No ad on the very first export: first impression matters for ratings.
                _export.value = ExportUi.Done(result, showAd = count >= 2)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _export.value = ExportUi.Hidden
                _messages.tryEmit(UiMessage(R.string.export_failed))
            }
        }
    }

    /** Closes the result dialog. Returns true once, after the 3rd export, to ask for a rating. */
    fun closeExport(): Boolean {
        _export.value = ExportUi.Hidden
        if (prefs.getInt(KEY_EXPORTS, 0) >= 3 && !prefs.getBoolean(KEY_REVIEW_ASKED, false)) {
            prefs.edit().putBoolean(KEY_REVIEW_ASKED, true).apply()
            return true
        }
        return false
    }

    fun saveTo(uri: Uri) = save {
        val file = (export.value as? ExportUi.Done)?.result?.files?.singleOrNull()
        if (file == null) {
            // Result lost (e.g. process death while the picker was open): remove the empty file.
            runCatching { DocumentsContract.deleteDocument(resolver(), uri) }
            return@save null
        }
        if (resolver().openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } == null) return@save null
        // The picker may have renamed the file (e.g. "scan (1).pdf"), so show the name it really got.
        val name = runCatching {
            resolver().query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: file.name
        savedMessage(name, file.length(), runCatching { placeOf(uri, DocumentsContract.getDocumentId(uri), false) }.getOrNull())
    }

    fun saveToFolder(tree: Uri) = save {
        val files = (export.value as? ExportUi.Done)?.result?.files ?: return@save null
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
        val ok = files.all { f ->
            val doc = DocumentsContract.createDocument(resolver(), parent, "image/jpeg", f.name) ?: return@all false
            resolver().openOutputStream(doc)?.use { out -> f.inputStream().use { it.copyTo(out) } } != null
        }
        if (!ok) return@save null
        val res = getApplication<Application>().resources
        val label = if (files.size == 1) files[0].name else res.getQuantityString(R.plurals.files_count, files.size, files.size)
        savedMessage(label, files.sumOf { it.length() }, runCatching { placeOf(tree, treeId, true) }.getOrNull())
    }

    private fun savedMessage(name: String, bytes: Long, place: String?): UiMessage {
        val size = formatSize(bytes)
        return if (place != null) UiMessage(R.string.saved_in, listOf(name, size, place), long = true)
        else UiMessage(R.string.saved_file, listOf(name, size), long = true)
    }

    /** Folder name when the provider exposes one, otherwise the storage app's name (e.g. "Drive"). */
    private fun placeOf(uri: Uri, documentId: String, isTree: Boolean): String? {
        val authority = uri.authority ?: return null
        SaveLocation.folder(authority, documentId, isTree)?.let { return it }
        val pm = getApplication<Application>().packageManager
        return pm.resolveContentProvider(authority, 0)?.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun save(block: () -> UiMessage?) {
        viewModelScope.launch {
            val msg = withContext(Dispatchers.IO) { runCatching(block).getOrNull() }
            _messages.tryEmit(msg ?: UiMessage(R.string.save_failed))
        }
    }

    private fun resolver() = getApplication<Application>().contentResolver

    /** Like [io], but blocks page actions meanwhile and shares the import lock so file names can't race. */
    private fun busyIo(block: () -> Unit) {
        viewModelScope.launch {
            importLock.withLock {
                _busy.value = true
                try {
                    withContext(Dispatchers.IO) { runCatching(block) }
                    _docs.value = withContext(Dispatchers.IO) { repo.list() }
                } finally {
                    _busy.value = false
                }
            }
        }
    }

    private fun io(block: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { block() }
            _docs.value = withContext(Dispatchers.IO) { repo.list() }
        }
    }

    private fun defaultName(): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH.mm", Locale.US).format(Date())
        return getApplication<Application>().getString(R.string.default_doc_name, stamp)
    }

    companion object {
        private const val KEY_FORMAT = "format"
        private const val KEY_LIMIT = "limit_kb"
        private const val KEY_MIN = "min_kb"
        private const val KEY_EXPORTS = "exports"
        private const val KEY_REVIEW_ASKED = "review_asked"
        private const val KEY_THEME = "theme"
        private const val KEY_HELP_SEEN = "help_seen"
    }
}
