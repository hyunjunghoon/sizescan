package io.github.hyunjunghoon.docscan.core

/** Turns a Storage Access Framework document id into a folder name people recognize. */
object SaveLocation {
    private const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"
    private const val DOWNLOADS = "com.android.providers.downloads.documents"

    /**
     * Folder that holds the saved file (or the picked folder itself when [isTree]), e.g. "Download"
     * or "Documents/Scans". Null when the provider doesn't expose a path (cloud drives, media ids).
     */
    fun folder(authority: String, documentId: String, isTree: Boolean): String? {
        val path = when {
            documentId.startsWith("raw:") -> documentId.removePrefix("raw:")
                .replace(Regex("^/storage/(emulated/\\d+|[^/]+)/?"), "")
            // Downloads ids are opaque numbers ("msf:123"); the files live in the Download folder.
            authority == DOWNLOADS -> return "Download"
            authority == EXTERNAL_STORAGE -> documentId.substringAfter(':', "")
            else -> return null
        }
        val parts = path.split('/').filter { it.isNotBlank() }.let { if (isTree) it else it.dropLast(1) }
        if (parts.isEmpty()) return null
        // The last two levels are enough to recognize the folder without flooding the message.
        return parts.takeLast(2).joinToString("/")
    }
}
